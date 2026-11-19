import os
import sys
import uuid
from pathlib import Path

PROJECT = Path(r'C:\Users\LENOVO\Documents\ESCUELA\8IDS2\SEGURIDAD DESAROLLO\Aguapp')
sys.path.insert(0,str(PROJECT))
os.environ.setdefault('DJANGO_SETTINGS_MODULE','config.settings')
os.environ['AGUAPP_HTTPS']='1'
import django
django.setup()

import logging
from django.contrib.auth.models import User
from django.db import transaction
from django.test import Client
from core.models import Report, Event, Audit

worker = User.objects.get(username='trabajador@aguapp.test',is_active=True,profile__role='trabajador',profile__zone='Centro')
owner = User.objects.get(username='ciudadano@aguapp.test',is_active=True,profile__role='ciudadano')
admin = User.objects.get(username='admin@aguapp.test',is_active=True,is_superuser=True)
token = uuid.uuid5(uuid.NAMESPACE_URL,'aguapp:reporte-demo-centro-clase:2026-11-04')
with transaction.atomic():
    report = Report.objects.filter(submission=token).first()
    created = report is None
    if created:
        report = Report(owner=owner,submission=token,kind='fuga',zone='Centro',
            address='Centro de Tecámac: punto de demostración en el mapa. Ubicación ficticia para exposición.',
            description='REPORTE DE DEMOSTRACIÓN: fuga de agua en la vía pública. Se asigna a Luis Demo para mostrar el seguimiento en clase; no representa un incidente real.',
            latitude='19.713000',longitude='-98.968000')
        report.full_clean()
        report.save()
        Event.objects.create(report=report,actor=owner,state='recibido',note='Reporte ficticio creado desde consola para la exposición, con coordenadas de demostración.')
        report.worker = worker
        report.state = 'asignado'
        report.full_clean()
        report.save()
        Event.objects.create(report=report,actor=admin,state='asignado',note='Asignado a Luis Demo, trabajador activo de Centro. Pendiente de iniciar atención.')
        Audit.objects.create(actor=admin,action='Reporte demo creado y asignado (consola)',target=report.folio,ip='127.0.0.1')
if created:
    logging.getLogger('aguapp').info('Reporte de demostración creado y asignado actor=%s target=%s worker=%s zona=Centro ip=127.0.0.1',admin.pk,report.folio,worker.pk)

client = Client()
try:
    client.force_login(worker)
    response = client.get('/api/mapa/reportes/',secure=True,HTTP_HOST='localhost')
    assert response.status_code == 200, response.status_code
    assert any(point['folio']==report.folio for point in response.json()['reports'])
finally:
    client.logout()
print('Creado:' if created else 'Ya existía:',report.folio)
print('Asignado a:',worker.get_full_name(),worker.username)
print('Estado:',report.get_state_display())
print('Coordenadas:',report.latitude,report.longitude)
print('Marcador verificado en API del trabajador.')
print('Detalle: https://aguapp.localhost:8443/reportes/'+str(report.pk)+'/')
