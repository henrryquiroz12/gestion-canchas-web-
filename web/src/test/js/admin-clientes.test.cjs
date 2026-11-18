const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const vm = require('node:vm');
const {resolve} = require('node:path');

function setup(reservations = false) {
    const elements = {};
    let focused;
    class Element {
        constructor() { this.children = []; this.listeners = {}; this.dataset = {}; this.attrs = {}; this.value = ''; this.textContent = ''; }
        appendChild(child) { this.children.push(child); return child; }
        replaceChildren(...children) { this.children = children.flatMap(c => c.fragment ? c.children : [c]); }
        setAttribute(key, value) { this.attrs[key] = value; }
        removeAttribute(key) { delete this.attrs[key]; }
        addEventListener(type, callback) { (this.listeners[type] ||= []).push(callback); }
        fire(type) { for (const cb of this.listeners[type] || []) cb({preventDefault() {}}); }
        focus() { focused = this; }
        cloneNode() { return new Element(); }
        querySelector() { return new Element(); }
    }
    for (const id of ['clientSearchForm', 'clientSearchCriterion', 'clientSearchValue', 'clientSearchResults', 'clientSearchError', 'clientSearchStatus', 'clearClientSearch']) {
        elements[id] = new Element();
    }
    elements.clientSearchCriterion.value = 'DNI';
    elements.clientSearchForm.dataset = {searchUrl: '/admin/api/clientes/buscar', clientsUrl: '/admin/clientes'};
    if (reservations) Object.assign(elements.clientSearchForm.dataset, {
        resultType: 'reservations', searchUrl: '/admin/reservas/buscar', detailsUrl: '/admin/reservas'
    });
    const requests = [];
    const timers = new Map();
    let timerId = 0;
    const context = vm.createContext({
        URL, AbortController,
        window: {location: {href: 'http://localhost/admin/clientes'}},
        document: {
            getElementById: id => elements[id],
            createElement: () => new Element(),
            createDocumentFragment: () => Object.assign(new Element(), {fragment: true}),
            addEventListener: (type, cb) => { context.ready = cb; }
        },
        setTimeout: cb => { timers.set(++timerId, cb); return timerId; },
        clearTimeout: id => timers.delete(id),
        fetch: (url, options) => new Promise(resolve => requests.push({url, options, resolve}))
    });
    vm.runInContext(readFileSync(resolve(__dirname, '../../main/resources/static/js/admin/clientes.js'), 'utf8'), context);
    context.ready();
    function type(value) {
        elements.clientSearchValue.focus();
        elements.clientSearchValue.value = value;
        elements.clientSearchValue.fire('input');
    }
    function runTimer() {
        const [id, callback] = [...timers.entries()].at(-1);
        timers.delete(id);
        return callback();
    }
    function respond(index, data, ok = true) { requests[index].resolve({ok, json: async () => data}); }
    return {elements, requests, timers, type, runTimer, respond, focused: () => focused};
}
const client = name => ({id: 1, fullName: name, dni: '00123456', username: 'cliente', email: 'cliente@gmail.com', active: true});

test('reservation search shares debounce and renders details without losing focus', async () => {
    const app = setup(true);
    assert.equal(app.requests.length, 0);
    app.type('001'); app.type('00123456');
    assert.equal(app.timers.size, 1);
    const task = app.runTimer();
    assert.equal(app.requests[0].url.pathname, '/admin/reservas/buscar');
    app.respond(0, [{id: 12, fullName: 'María Peña', dni: '00123456', courtName: 'Cancha 1',
        date: '2026-11-10', startTime: '10:00:00', endTime: '11:00:00', amount: 40, status: 'COMPLETADO'}]);
    await task;
    const row = app.elements.clientSearchResults.children[0];
    assert.equal(row.children[0].textContent, 'RES-12');
    assert.equal(row.children[3].textContent, '10/11/2026');
    assert.equal(row.children[7].children[0].href, '/admin/reservas/12');
    assert.equal(app.focused(), app.elements.clientSearchValue);
    app.elements.clearClientSearch.fire('click');
    assert.equal(app.elements.clientSearchResults.children.length, 0);
});

test('initial section and cleared search are empty and make no request', async () => {
    const app = setup();
    assert.equal(app.requests.length, 0);
    assert.equal(app.timers.size, 0);
    assert.equal(app.elements.clientSearchResults.children.length, 0);
    app.type('1');
    const pending = app.runTimer();
    app.elements.clearClientSearch.fire('click');
    app.respond(0, [client('Respuesta antigua')]);
    await pending;
    assert.equal(app.elements.clientSearchResults.children.length, 0);
    assert.equal(app.timers.size, 0);
    assert.equal(app.focused(), app.elements.clientSearchValue);
});

test('typing debounces, narrows results without navigation and retains input focus', async () => {
    const app = setup();
    app.type('0'); app.type('00'); app.type('001');
    assert.equal(app.timers.size, 1);
    assert.equal(app.requests.length, 0);
    const first = app.runTimer();
    app.respond(0, [client('María Peña'), client('Ana Peña')]);
    await first;
    assert.equal(app.elements.clientSearchResults.children.length, 2);
    app.type('00123456');
    const next = app.runTimer();
    assert.equal(app.requests[1].url.searchParams.get('valor'), '00123456');
    app.respond(1, [client('María Peña')]);
    await next;
    assert.equal(app.elements.clientSearchResults.children.length, 1);
    assert.equal(app.focused(), app.elements.clientSearchValue);
    assert.equal(app.elements.clientSearchValue.value, '00123456');
});

test('out-of-order responses never overwrite the latest results', async () => {
    const app = setup();
    app.type('1'); const old = app.runTimer();
    app.type('12'); const recent = app.runTimer();
    assert.equal(app.requests[0].options.signal.aborted, true);
    app.respond(1, [client('Resultado nuevo')]); await recent;
    app.respond(0, [client('Resultado antiguo')]); await old;
    assert.equal(app.elements.clientSearchResults.children[0].children[0].textContent, 'Resultado nuevo');
});

test('name mode accepts accents, clears old search and rejects numbers', async () => {
    const app = setup();
    app.type('123');
    app.elements.clientSearchCriterion.value = 'NOMBRE';
    app.elements.clientSearchCriterion.fire('change');
    assert.equal(app.timers.size, 0);
    assert.equal(app.elements.clientSearchValue.maxLength, 100);
    app.type('María Muñoz');
    const task = app.runTimer();
    assert.equal(app.requests[0].url.searchParams.get('criterio'), 'NOMBRE');
    app.respond(0, []); await task;
    app.type('María9');
    assert.equal(app.timers.size, 0);
    assert.ok(app.elements.clientSearchError.textContent);
});
