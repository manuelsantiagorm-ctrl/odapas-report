// Uso: node database/generar-diagrama.cjs metadatos.json salida.dbml
// Metadatos se generan con psql -At -f database/exportar_modelo.sql -o metadatos.json
const fs = require('node:fs');
const [entrada, salida] = process.argv.slice(2);
if (!entrada || !salida) throw new Error('Indicar archivo JSON de metadatos y salida DBML');
const {tables} = JSON.parse(fs.readFileSync(entrada, 'utf8').replace(/^\uFEFF/, ''));
const lines = ["Project AguApp {", "  database_type: 'PostgreSQL'", "  Note: 'Modelo 3FN: ciudadanos, registro de personal y atención de reparaciones. Generado del esquema PostgreSQL probado.'", "}", ''];
const refs = [];
for (const table of tables) {
  const constraints = table.constraints || [];
  const pk = constraints.find(c => c.kind === 'p');
  const uniques = constraints.filter(c => c.kind === 'u');
  lines.push(`Table ${table.name} {`);
  for (const col of table.columns) {
    const settings = [];
    if (pk?.columns.length === 1 && pk.columns[0] === col.name) settings.push('pk');
    if (col.not_null && !settings.includes('pk')) settings.push('not null');
    if (uniques.some(c => c.columns.length === 1 && c.columns[0] === col.name)) settings.push('unique');
    if (col.default === 'CURRENT_TIMESTAMP') settings.push('default: `CURRENT_TIMESTAMP`');
    else if (col.default === 'true' || col.default === 'false') settings.push(`default: ${col.default}`);
    else if (col.default?.startsWith("'")) settings.push(`default: ${col.default.split('::')[0]}`);
    const type = col.type.replace('character varying', 'varchar').replace('timestamp with time zone', 'timestamptz').replace('numeric', 'decimal');
    lines.push(`  ${col.name} ${type}${settings.length ? ' [' + settings.join(', ') + ']' : ''}`);
  }
  const composite = [...(pk?.columns.length > 1 ? [{...pk, setting:'pk'}] : []),
    ...uniques.filter(c => c.columns.length > 1).map(c => ({...c,setting:'unique'}))];
  const regular = table.indexes || [];
  if (composite.length || regular.length) {
    lines.push('', '  indexes {');
    for (const c of composite) lines.push(`    (${c.columns.join(', ')}) [${c.setting}]`);
    for (const i of regular) lines.push(`    ${i.columns.length === 1 ? i.columns[0] : '(' + i.columns.join(', ') + ')'} [name: '${i.name}']`);
    lines.push('  }');
  }
  const checks = constraints.filter(c => c.kind === 'c');
  if (checks.length) {
    lines.push('', '  checks {');
    for (const c of checks) {
      const expr = c.definition.replace(/^CHECK\s*\(/, '').replace(/\)$/, '').replace(/`/g, '');
      lines.push('    `' + expr + '`');
    }
    lines.push('  }');
  }
  lines.push('}', '');
  for (const fk of constraints.filter(c => c.kind === 'f')) {
    const uniqueFk = pk?.columns.length === 1 && pk.columns[0] === fk.columns[0]
      || uniques.some(c => c.columns.length === 1 && c.columns[0] === fk.columns[0]);
    const from = table.name + '.' + fk.columns[0];
    const to = fk.target + '.' + fk.target_columns[0];
    const deletion = fk.delete_action === 'c' ? ' [delete: cascade]' : '';
    refs.push(uniqueFk ? `Ref: ${to} - ${from}${deletion}` : `Ref: ${from} > ${to}${deletion}`);
  }
}
lines.push('// Relaciones: > muchos a uno; - uno a uno (perfiles opcionales).', ...refs, '');
const groups = {
  Identidad_y_registro: ['usuario','rol','usuario_rol','ciudadano','organismo','empleado','reparador','especialidad','reparador_especialidad','version_terminos','aceptacion_terminos','recuperacion_password'],
  Suministro: ['colonia','estado_servicio','calendario_suministro'],
  Reportes_ciudadanos: ['tipo_reporte','reporte','verificacion_reporte','evidencia_reporte'],
  Atencion_y_reparaciones: ['cuadrilla','cuadrilla_miembro','prioridad','estado_orden','orden_trabajo','asignacion_orden','resultado_reparacion','reparacion','evidencia_reparacion','unidad_medida','material','consumo_material','historial_orden']
};
for (const [name, members] of Object.entries(groups)) {
  lines.push(`TableGroup ${name} {`, ...members.map(t => '  ' + t), '}', '');
}
fs.writeFileSync(salida, lines.join('\n'), 'utf8');
console.log(`DBML generado: ${tables.length} tablas y ${refs.length} relaciones`);
