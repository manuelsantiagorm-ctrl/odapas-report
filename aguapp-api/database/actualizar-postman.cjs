const fs = require('node:fs');
const path = 'postman/AguApp-API.postman_collection.json';
const collection = JSON.parse(fs.readFileSync(path, 'utf8').replace(/^\uFEFF/, ''));
function request(name, route, status, checks = []) {
  return {name, request:{method:'GET',header:[],url:'{{baseUrl}}'+route},event:[{listen:'test',script:{type:'text/javascript',exec:[
    `pm.test('Status ${status}', () => pm.response.to.have.status(${status}));`, ...checks
  ]}}]};
}
collection.info.description = 'Cuatro controllers de consulta JDBC: colonias, reportes, reparadores aprobados y órdenes de trabajo. Usar una base demo con migraciones V1 a V5.';
collection.item = collection.item.filter(folder => !['ReparadorController','OrdenTrabajoController'].includes(folder.name));
collection.item.push({name:'ReparadorController',item:[
  request('Listar reparadores aprobados','/api/reparadores',200,["pm.test('Dos aprobados', () => pm.expect(pm.response.json().length).to.eql(2));"]),
  request('Consultar reparador','/api/reparadores/usr-reparador',200,["pm.test('Perfil técnico', () => pm.expect(pm.response.json().id).to.eql('usr-reparador'));"]),
  request('Aspirante pendiente no aparece','/api/reparadores/usr-pendiente',404)
]});
collection.item.push({name:'OrdenTrabajoController',item:[
  request('Listar órdenes','/api/ordenes-trabajo',200,["pm.test('Una orden', () => pm.expect(pm.response.json().length).to.eql(1));"]),
  request('Orden por ID y estado actual','/api/ordenes-trabajo/ot1',200,["pm.test('Estado reparada', () => pm.expect(pm.response.json().estado).to.eql('reparada'));","pm.test('Origen r1', () => pm.expect(pm.response.json().reporteId).to.eql('r1'));"]),
  request('Filtrar por reparador asignado','/api/ordenes-trabajo?reparadorId=usr-reparador',200,["pm.test('Una asignada', () => pm.expect(pm.response.json().length).to.eql(1));"]),
  request('Ciudadano sin órdenes asignadas','/api/ordenes-trabajo?reparadorId=usr-demo',200,["pm.test('Sin órdenes', () => pm.expect(pm.response.json().length).to.eql(0));"]),
  request('Filtrar por estado','/api/ordenes-trabajo?estado=reparada',200,["pm.test('Solo reparadas', () => pm.response.json().forEach(o => pm.expect(o.estado).to.eql('reparada')));"]),
  request('Orden inexistente','/api/ordenes-trabajo/no-existe',404)
]});
fs.writeFileSync(path, JSON.stringify(collection,null,2)+'\n','utf8');
console.log('Colección Postman actualizada con los cuatro controllers');
