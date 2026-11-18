const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const vm = require('node:vm');

function setup() {
    let now = 0, ready;
    const intervals = [], pending = [];
    const element = () => ({textContent: '', disabled: false, hidden: false, listeners: {},
        addEventListener(type, callback) { this.listeners[type] = callback; }});
    const timer = element(), message = element(), cancel = element(), resume = element(), confirm = element();
    const banner = {
        dataset: {state: 'ACTIVE', remaining: '900', token: 'hold-one', statusUrl: '/retenciones',
            cancelUrl: '/retenciones/cancelar', csrfName: '_csrf', csrfToken: 'csrf-token'},
        querySelector(selector) { return {'[data-hold-timer]': timer, '[data-hold-message]': message,
            '[data-hold-cancel]': cancel, '[data-hold-resume]': resume}[selector]; }
    };
    const context = vm.createContext({
        performance: {now: () => now}, URLSearchParams,
        document: {getElementById: () => banner, querySelectorAll: () => [confirm],
            addEventListener(type, callback) { if (type === 'DOMContentLoaded') ready = callback; }},
        setInterval(callback, delay) { intervals.push({callback, delay}); },
        fetch: (url, options) => new Promise(resolve => pending.push({url, options, resolve}))
    });
    vm.runInContext(readFileSync(resolve(__dirname, '../../main/resources/static/js/reservation-hold.js'), 'utf8'), context);
    ready();
    return {timer, message, cancel, resume, confirm, pending,
        advance(ms) { now += ms; intervals.find(i => i.delay === 1000).callback(); },
        refresh() { return intervals.find(i => i.delay === 15000).callback(); },
        cancelHold() { return cancel.listeners.click(); },
        respond(index, data) { pending[index].resolve({ok: true, json: async () => data}); }
    };
}

test('countdown keeps an absolute deadline and never renews it on a tick', () => {
    const ui = setup();
    assert.equal(ui.timer.textContent, '15:00');
    ui.advance(60000);
    assert.equal(ui.timer.textContent, '14:00');
    assert.equal(ui.confirm.disabled, false);
    ui.advance(14 * 60000);
    assert.equal(ui.timer.textContent, '0:00');
    assert.equal(ui.confirm.disabled, true);
    assert.equal(ui.resume.hidden, true);
    assert.match(ui.message.textContent, /terminó/);
});

test('cancel includes CSRF and does not let a pending response reactivate the hold', async () => {
    const ui = setup();
    const oldStatus = ui.refresh();
    const cancellation = ui.cancelHold();
    assert.equal(ui.pending[1].options.method, 'POST');
    assert.equal(ui.pending[1].options.body.get('_csrf'), 'csrf-token');
    assert.equal(ui.pending[1].options.body.get('token'), 'hold-one');
    ui.respond(1, {status: 'CANCELLED'});
    await cancellation;
    ui.respond(0, {token: 'hold-one', status: 'ACTIVE', remainingSeconds: 890});
    await oldStatus;
    assert.equal(ui.confirm.disabled, true);
    assert.match(ui.message.textContent, /Cancelaste/);
});

test('server expiry disables confirmation and a different token cannot replace this selection', async () => {
    const ui = setup();
    const status = ui.refresh();
    ui.respond(0, {status: 'EXPIRED', token: 'hold-one', remainingSeconds: 0});
    await status;
    assert.equal(ui.confirm.disabled, true);
    const replaced = ui.refresh();
    ui.respond(1, {status: 'ACTIVE', token: 'different-hold', remainingSeconds: 900});
    await replaced;
    assert.equal(ui.confirm.disabled, true);
    assert.equal(ui.resume.hidden, true);
});
