const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const vm = require('node:vm');
const main = resolve(__dirname, '../../main');

// Exercise the production scripts with a small DOM adapter and deferred HTTP responses.
function setup(backendError = false) {
    const ids = {};
    let focused;
    class Element {
        constructor(id, value = '') {
            this.id = id;
            this.name = id;
            this.value = value;
            this.textContent = '';
            this.dataset = {};
            this.attrs = {};
            this.listeners = {};
            this.children = [];
            this.disabled = false;
            this.maxLength = -1;
            this.type = 'text';
            this.required = false;
            const classes = new Set();
            this.classList = {
                add: name => classes.add(name), remove: name => classes.delete(name),
                contains: name => classes.has(name),
                toggle: (name, yes) => yes ? classes.add(name) : classes.delete(name)
            };
            ids[id] = this;
        }
        get validity() { return {valid: !this.required || !!this.value}; }
        get validationMessage() { return 'Campo obligatorio'; }
        setAttribute(key, value) { this.attrs[key] = value; }
        getAttribute(key) { return this.attrs[key]; }
        removeAttribute(key) { delete this.attrs[key]; }
        focus() { focused = this.id; }
        addEventListener(type, callback) { (this.listeners[type] ||= []).push(callback); }
        async fire(type) {
            for (const callback of this.listeners[type] || []) {
                await callback({preventDefault() {}});
            }
        }
        querySelector() { return null; }
        closest() { return null; }
        querySelectorAll(selector) {
            if (selector === 'input, select, textarea') return this.children;
            return this.children.filter(child => child.classList.contains('is-invalid') || child.backendError);
        }
        appendChild(child) { this.children.push(child); }
        set innerHTML(value) { this.children = []; }
    }
    const form = new Element('registroForm');
    form.dataset.dniUrl = '/registro/validar-dni';
    const groups = [1, 2, 3].map(n => new Element(`stepFieldset${n}`));
    for (const id of ['btnNext1', 'btnNext2', 'btnPrev2', 'btnPrev3', 'btnSubmit', 'progBar1', 'progBar2', 'progBar3', 'wizardStepLabel', 'generoError']) new Element(id);
    const values = [
        {fullName: 'María Peña', dni: '00123456'},
        {phoneNumber: '912345678', department: 'LIMA', province: 'Lima', address: ''},
        {email: 'ana@GMAIL.COM', username: 'maria', password: 'abcdef', confirmPassword: 'abcdef'}
    ];
    values.forEach((values, index) => {
        Object.entries(values).forEach(([id, value]) => {
            const input = new Element(id, value);
            input.required = id !== 'address';
            input.form = form;
            new Element(id + 'Error');
            groups[index].children.push(input);
        });
    });
    if (backendError) ids.dni.backendError = true;
    const radio = new Element('gender');
    radio.checked = true;
    const ready = [];
    const requests = [];
    const document = {
        getElementById: id => ids[id],
        addEventListener: (type, callback) => ready.push(callback),
        querySelectorAll: selector => selector.includes('gender') ? [radio] : [],
        createElement: tag => new Element(`new-${tag}-${Object.keys(ids).length}`)
    };
    const constants = readFileSync(resolve(main, 'java/com/canchas/web/validation/AccountRules.java'), 'utf8');
    const rules = {};
    for (const match of constants.matchAll(/public static final String (NAME|DNI|PHONE|EMAIL) = ("(?:\\.|[^"\\])*");/g)) {
        rules[{NAME: 'fullName', DNI: 'dni', PHONE: 'phoneNumber', EMAIL: 'email'}[match[1]]] = JSON.parse(match[2]);
    }
    let submitted = false;
    const context = vm.createContext({
        window: {ACCOUNT_RULES: rules, location: {href: 'http://localhost/registro'}, PROVINCIAS_MAP: {LIMA: ['Lima']}},
        document, URL, HTMLFormElement: {prototype: {submit() { submitted = true; }}},
        fetch: () => new Promise((resolve, reject) => requests.push({resolve, reject}))
    });
    for (const file of ['core/account-validation.js', 'registro-wizard.js']) {
        vm.runInContext(readFileSync(resolve(main, 'resources/static/js', file), 'utf8'), context);
    }
    ready.forEach(callback => callback());
    return {
        ids, groups, requests, context,
        focused: () => focused, submitted: () => submitted,
        respond(available) { requests.at(-1).resolve({ok: true, json: async () => ({available})}); }
    };
}

test('duplicate DNI stays in step 1, waits for the server and preserves data; correction advances', async () => {
    const app = setup();
    const next = app.ids.btnNext1.fire('click');
    assert.equal(app.ids.btnNext1.disabled, true);
    assert.equal(app.groups[1].hidden, true);
    app.respond(false);
    await next;
    assert.equal(app.groups[0].hidden, false);
    assert.equal(app.focused(), 'dni');
    assert.match(app.ids.dniError.textContent, /registrado/);
    assert.equal(app.ids.fullName.value, 'María Peña');
    assert.equal(app.ids.dni.value, '00123456');
    app.ids.dni.value = '00123457';
    const corrected = app.ids.btnNext1.fire('click');
    app.respond(true);
    await corrected;
    assert.equal(app.groups[1].hidden, false);
    assert.equal(app.ids.dniError.textContent, '');
});

test('stale response and failed request cannot advance', async () => {
    const app = setup();
    const next = app.ids.btnNext1.fire('click');
    app.ids.dni.value = '00123457';
    app.respond(true);
    await next;
    assert.equal(app.groups[0].hidden, false);
    const retry = app.ids.btnNext1.fire('click');
    app.requests.at(-1).reject(new Error('offline'));
    await retry;
    assert.equal(app.groups[0].hidden, false);
    assert.equal(app.ids.btnNext1.disabled, false);
    assert.match(app.ids.dniError.textContent, /comprobar/);
});

test('each step and the final submission validate all fields again', async () => {
    const app = setup();
    let next = app.ids.btnNext1.fire('click');
    app.respond(true);
    await next;
    app.ids.phoneNumber.value = '812345678';
    await app.ids.btnNext2.fire('click');
    assert.equal(app.groups[1].hidden, false);
    app.ids.phoneNumber.value = '912345678';
    await app.ids.btnNext2.fire('click');
    app.ids.fullName.value = 'Ana9';
    await app.ids.registroForm.fire('submit');
    assert.equal(app.groups[0].hidden, false);
    assert.equal(app.submitted(), false);
});

test('final duplicate returns to step 1; backend DNI errors reopen step 1', async () => {
    const app = setup();
    const next = app.ids.btnNext1.fire('click');
    app.respond(true);
    await next;
    await app.ids.btnNext2.fire('click');
    const submit = app.ids.registroForm.fire('submit');
    app.respond(false);
    await submit;
    assert.equal(app.groups[0].hidden, false);
    assert.equal(app.submitted(), false);
    assert.equal(setup(true).groups[0].hidden, false);
});

test('browser uses the server patterns for names, exact domains and untrimmed digits', () => {
    const app = setup();
    const validate = index => app.context.window.AccountValidation.validate(app.groups[index]);
    for (const [id, index, invalid] of [
        ['fullName', 0, ['Ana9', 'Ana-Peña', ' Ana']],
        ['dni', 0, ['12345678 ', '+1234567', '１２３４５６７８']],
        ['phoneNumber', 1, ['812345678', '912345678 ', '+912345678']],
        ['email', 2, ['a@@gmail.com', 'a@sub.gmail.com', 'a@gmail', 'a@gmail.com ', 'a..b@gmail.com']]
    ]) {
        const original = app.ids[id].value;
        for (const value of invalid) {
            app.ids[id].value = value;
            assert.equal(validate(index), false, value);
        }
        app.ids[id].value = original;
        assert.equal(validate(index), true, original);
    }
});
