/* Salon dashboard - plain JavaScript, koi build step nahi chahiye */

const $ = (id) => document.getElementById(id);
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const money = (n) => '₹' + Number(n || 0).toLocaleString('en-IN');
const fmtDate = (iso) => iso ? new Date(iso).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '';
const fmtTime = (iso) => iso ? new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : '';

let customers = [];
let services = [];
let appointments = [];
let lastOnline = null;

/* ---------- API helper ---------- */
async function api(path, method = 'GET', body) {
  const res = await fetch(path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined
  });
  if (res.status === 401 || res.status === 403) { location.href = '/login.html'; throw new Error('login'); }
  let data = null;
  const text = await res.text();
  if (text) { try { data = JSON.parse(text); } catch (e) { data = text; } }
  if (!res.ok) throw new Error((data && data.error) || 'Kuch galat ho gaya');
  return data;
}

function toast(msg) {
  const t = $('toast');
  t.textContent = msg;
  t.classList.add('show');
  setTimeout(() => t.classList.remove('show'), 2600);
}

/* ---------- modal ---------- */
let onSave = null;
function openModal(title, html, saveFn) {
  $('mTitle').textContent = title;
  $('mBody').innerHTML = html;
  $('mErr').textContent = '';
  onSave = saveFn;
  $('modalBg').classList.add('open');
}
function closeModal() { $('modalBg').classList.remove('open'); onSave = null; }
$('mCancel').onclick = closeModal;
$('mSave').onclick = async () => {
  if (!onSave) return;
  try { await onSave(); closeModal(); }
  catch (e) { $('mErr').textContent = e.message; }
};

/* ---------- tabs ---------- */
document.querySelectorAll('.tab').forEach(btn => {
  btn.onclick = () => {
    document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    $(btn.dataset.tab).classList.add('active');
    $('pageTitle').textContent = btn.dataset.title || btn.textContent;
    loadTab(btn.dataset.tab);
  };
});
function loadTab(t) {
  if (t === 'dash') loadDashboard();
  if (t === 'appts') loadAppointments();
  if (t === 'customers') loadCustomers();
  if (t === 'services') loadServices();
  if (t === 'wa') loadLogs();
}

$('logoutBtn').onclick = async () => { await fetch('/logout', { method: 'POST' }); location.href = '/login.html'; };

/* ---------- dashboard ---------- */
async function loadDashboard() {
  const d = await api('/api/dashboard');
  $('sToday').textContent = d.appointmentsToday;
  $('sCustomers').textContent = d.customers;
  $('sServices').textContent = d.services;
  $('sRevenue').textContent = money(d.revenueThisMonth);

  // Nayi online bookings (customer ne booking page se ki)
  $('sOnline').textContent = d.newOnline;
  $('onlineBox').style.display = d.newOnline ? 'block' : 'none';
  $('onlineTable').innerHTML = '<tr><th>Customer</th><th>Service</th><th>Kab</th><th>Note</th><th></th></tr>' +
    d.newOnlineList.map(a => `<tr>
      <td>${esc(a.customer.name)}<br><small style="color:#6b7380">${esc(a.customer.phone)}</small></td>
      <td>${esc(a.service.name)}</td><td>${fmtDate(a.startTime)}</td><td>${esc(a.notes)}</td>
      <td style="text-align:right;white-space:nowrap">
        <button class="btn small wa" onclick="sendWa(${a.id})">WhatsApp</button>
        <button class="btn small" onclick="markSeen(${a.id})">Dekh liya</button>
      </td></tr>`).join('');
  if (lastOnline !== null && d.newOnline > lastOnline) toast('🆕 Nayi online booking aayi!');
  lastOnline = d.newOnline;
  $('waBanner').innerHTML = d.whatsappConfigured ? '' :
    '<div class="banner warn">WhatsApp abhi connect nahi hai. <code>.env</code> mein WHATSAPP_TOKEN aur WHATSAPP_PHONE_NUMBER_ID daalo.</div>';
  $('todayTable').innerHTML = d.todayList.length
    ? '<tr><th>Time</th><th>Customer</th><th>Service</th><th>Status</th></tr>' +
      d.todayList.map(a => `<tr><td>${fmtTime(a.startTime)}</td><td>${esc(a.customer.name)}</td><td>${esc(a.service.name)}</td><td><span class="badge ${a.status}">${a.status}</span></td></tr>`).join('')
    : '<tr><td class="empty">Aaj koi appointment nahi hai.</td></tr>';
}

/* ---------- customers ---------- */
async function loadCustomers() {
  customers = await api('/api/customers');
  renderCustomers();
}
function renderCustomers() {
  const q = $('custSearch').value.trim().toLowerCase();
  const list = customers.filter(c => !q || c.name.toLowerCase().includes(q) || (c.phone || '').includes(q));
  $('custTable').innerHTML = list.length
    ? '<tr><th>Naam</th><th>Phone</th><th>Email</th><th></th></tr>' +
      list.map(c => `<tr>
        <td>${esc(c.name)}</td><td>${esc(c.phone)}</td><td>${esc(c.email)}</td>
        <td style="text-align:right;white-space:nowrap">
          <button class="btn small" onclick="editCustomer(${c.id})">Edit</button>
          <button class="btn small danger" onclick="delCustomer(${c.id})">Delete</button>
        </td></tr>`).join('')
    : '<tr><td class="empty">Koi customer nahi mila.</td></tr>';
}
$('custSearch').oninput = renderCustomers;
$('newCust').onclick = () => customerForm();
window.editCustomer = (id) => customerForm(customers.find(c => c.id === id));
window.delCustomer = async (id) => {
  if (!confirm('Customer delete karna hai?')) return;
  try { await api('/api/customers/' + id, 'DELETE'); toast('Delete ho gaya'); loadCustomers(); }
  catch (e) { toast(e.message); }
};
function customerForm(c) {
  c = c || {};
  openModal(c.id ? 'Customer edit' : 'Naya customer', `
    <label>Naam</label><input id="fName" value="${esc(c.name)}">
    <label>Phone</label><input id="fPhone" value="${esc(c.phone)}" placeholder="9876543210">
    <label>Email (optional)</label><input id="fEmail" value="${esc(c.email)}">
    <label>Notes (optional)</label><textarea id="fNotes" rows="2">${esc(c.notes)}</textarea>`,
    async () => {
      const body = { name: $('fName').value, phone: $('fPhone').value, email: $('fEmail').value, notes: $('fNotes').value };
      await api(c.id ? '/api/customers/' + c.id : '/api/customers', c.id ? 'PUT' : 'POST', body);
      toast('Save ho gaya'); loadCustomers();
    });
}

/* ---------- services ---------- */
async function loadServices() {
  services = await api('/api/services');
  $('svcTable').innerHTML = services.length
    ? '<tr><th>Service</th><th>Price</th><th>Time</th><th>Status</th><th></th></tr>' +
      services.map(s => `<tr>
        <td>${esc(s.name)}</td><td>${money(s.price)}</td><td>${s.durationMinutes} min</td>
        <td>${s.active ? 'Active' : 'Inactive'}</td>
        <td style="text-align:right;white-space:nowrap">
          <button class="btn small" onclick="editService(${s.id})">Edit</button>
          <button class="btn small danger" onclick="delService(${s.id})">Delete</button>
        </td></tr>`).join('')
    : '<tr><td class="empty">Koi service nahi hai.</td></tr>';
}
$('newSvc').onclick = () => serviceForm();
window.editService = (id) => serviceForm(services.find(s => s.id === id));
window.delService = async (id) => {
  if (!confirm('Service delete karni hai?')) return;
  try { await api('/api/services/' + id, 'DELETE'); toast('Delete ho gaya'); loadServices(); }
  catch (e) { toast(e.message); }
};
function serviceForm(s) {
  s = s || { active: true, durationMinutes: 30 };
  openModal(s.id ? 'Service edit' : 'Nayi service', `
    <label>Naam</label><input id="fName" value="${esc(s.name)}">
    <label>Price (₹)</label><input id="fPrice" type="number" min="0" value="${esc(s.price)}">
    <label>Time (minutes)</label><input id="fDur" type="number" min="5" value="${s.durationMinutes}">
    <label>Status</label>
    <select id="fActive"><option value="true" ${s.active ? 'selected' : ''}>Active</option><option value="false" ${!s.active ? 'selected' : ''}>Inactive</option></select>`,
    async () => {
      const body = { name: $('fName').value, price: $('fPrice').value, durationMinutes: Number($('fDur').value), active: $('fActive').value === 'true' };
      await api(s.id ? '/api/services/' + s.id : '/api/services', s.id ? 'PUT' : 'POST', body);
      toast('Save ho gaya'); loadServices();
    });
}

/* ---------- appointments ---------- */
async function loadAppointments() {
  [appointments, customers, services] = await Promise.all([api('/api/appointments'), api('/api/customers'), api('/api/services')]);
  $('apptTable').innerHTML = appointments.length
    ? '<tr><th>Kab</th><th>Customer</th><th>Service</th><th>Price</th><th>Status</th><th></th></tr>' +
      appointments.map(a => `<tr>
        <td>${fmtDate(a.startTime)}</td><td>${esc(a.customer.name)}<br><small style="color:#6b7280">${esc(a.customer.phone)}</small></td>
        <td>${esc(a.service.name)}</td><td>${money(a.price)}</td>
        <td><span class="badge ${a.status}">${a.status}</span>${a.source === 'ONLINE' ? ' <span class="badge ONLINE">ONLINE</span>' : ''}${a.source === 'ONLINE' && a.seen === false ? ' <span class="badge NEW">NEW</span>' : ''}${a.whatsappSent ? ' <span title="WhatsApp bhej diya">✅</span>' : ''}</td>
        <td style="text-align:right;white-space:nowrap">
          <button class="btn small wa" onclick="sendWa(${a.id})">WhatsApp</button>
          ${a.status === 'SCHEDULED' ? `<button class="btn small" onclick="setStatus(${a.id},'COMPLETED')">Done</button>
          <button class="btn small danger" onclick="setStatus(${a.id},'CANCELLED')">Cancel</button>` : ''}
          <button class="btn small" onclick="editAppt(${a.id})">Edit</button>
          <button class="btn small danger" onclick="delAppt(${a.id})">🗑</button>
        </td></tr>`).join('')
    : '<tr><td class="empty">Abhi koi appointment nahi hai.</td></tr>';
}
$('newAppt').onclick = async () => {
  if (!customers.length || !services.length) { [customers, services] = await Promise.all([api('/api/customers'), api('/api/services')]); }
  apptForm();
};
window.editAppt = (id) => apptForm(appointments.find(a => a.id === id));
window.delAppt = async (id) => {
  if (!confirm('Appointment delete karni hai?')) return;
  await api('/api/appointments/' + id, 'DELETE'); toast('Delete ho gaya'); loadAppointments();
};
window.markSeen = async (id) => {
  try { await api('/api/appointments/' + id + '/seen', 'PATCH'); loadDashboard(); }
  catch (e) { toast(e.message); }
};
window.setStatus = async (id, status) => {
  await api('/api/appointments/' + id + '/status', 'PATCH', { status }); loadAppointments();
};
window.sendWa = async (id) => {
  try {
    const r = await api('/api/appointments/' + id + '/whatsapp', 'POST');
    toast(r.status === 'SENT' ? 'WhatsApp bhej diya ✅' : r.status === 'NOT_CONFIGURED' ? 'Pehle .env mein WhatsApp keys daalo' : 'Bhej nahi paye: History tab dekho');
    loadAppointments();
  } catch (e) { toast(e.message); }
};
function toLocalInput(iso) {
  const d = iso ? new Date(iso) : new Date(Date.now() + 3600e3);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}
function apptForm(a) {
  a = a || {};
  const custOpts = customers.map(c => `<option value="${c.id}" ${a.customer && a.customer.id === c.id ? 'selected' : ''}>${esc(c.name)} (${esc(c.phone)})</option>`).join('');
  const svcOpts = services.filter(s => s.active || (a.service && a.service.id === s.id))
    .map(s => `<option value="${s.id}" data-price="${s.price}" ${a.service && a.service.id === s.id ? 'selected' : ''}>${esc(s.name)} - ${money(s.price)}</option>`).join('');
  if (!customers.length) { toast('Pehle ek customer add karo'); return; }
  openModal(a.id ? 'Appointment edit' : 'Nayi appointment', `
    <label>Customer</label><select id="fCust">${custOpts}</select>
    <label>Service</label><select id="fSvc">${svcOpts}</select>
    <label>Date & time</label><input id="fWhen" type="datetime-local" value="${toLocalInput(a.startTime)}">
    <label>Price (₹)</label><input id="fPrice" type="number" min="0" value="${a.price ?? ''}" placeholder="khali = service ka price">
    <label>Notes (optional)</label><textarea id="fNotes" rows="2">${esc(a.notes)}</textarea>`,
    async () => {
      const body = {
        customerId: Number($('fCust').value), serviceId: Number($('fSvc').value),
        startTime: $('fWhen').value, price: $('fPrice').value === '' ? null : $('fPrice').value,
        status: a.status || 'SCHEDULED', notes: $('fNotes').value
      };
      await api(a.id ? '/api/appointments/' + a.id : '/api/appointments', a.id ? 'PUT' : 'POST', body);
      toast('Save ho gaya'); loadAppointments();
    });
}

/* ---------- whatsapp ---------- */
async function loadLogs() {
  const logs = await api('/api/whatsapp/logs');
  $('waTable').innerHTML = logs.length
    ? '<tr><th>Time</th><th></th><th>Phone</th><th>Message</th><th>Status</th></tr>' +
      logs.map(l => `<tr><td>${fmtDate(l.createdAt)}</td><td>${l.direction === 'IN' ? '⬅️' : '➡️'}</td><td>${esc(l.phone)}</td>
        <td>${esc(l.body)}${l.error ? `<br><small style="color:#b91c1c">${esc(l.error)}</small>` : ''}</td>
        <td><span class="badge ${l.status}">${l.status}</span></td></tr>`).join('')
    : '<tr><td class="empty">Abhi koi message nahi hai.</td></tr>';
}
$('waSend').onclick = async () => {
  const to = $('waTo').value.trim(), text = $('waText').value.trim();
  if (!to || !text) { toast('Phone aur message dono likho'); return; }
  const r = await api('/api/whatsapp/send', 'POST', { to, text });
  toast(r.status === 'SENT' ? 'Bhej diya ✅' : r.status === 'NOT_CONFIGURED' ? 'Pehle .env mein WhatsApp keys daalo' : 'Fail: neeche error dekho');
  if (r.status === 'SENT') $('waText').value = '';
  loadLogs();
};

/* ---------- booking link + auto refresh ---------- */
$('bookLink').textContent = location.origin + '/book.html';
$('copyLink').onclick = async () => {
  try { await navigator.clipboard.writeText(location.origin + '/book.html'); toast('Link copy ho gaya'); }
  catch (e) { toast('Link ko haath se copy kar lo'); }
};
// Dashboard khula ho to har 30 second mein nayi bookings check karo
setInterval(() => {
  if ($('dash').classList.contains('active') && !document.hidden) loadDashboard().catch(() => {});
}, 30000);

/* ---------- start ---------- */
(async () => {
  try {
    const me = await api('/api/me');
    $('who').textContent = me.username;
    $('avatar').textContent = (me.username || 'A').charAt(0);
    $('todayDate').textContent = new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    loadDashboard();
  } catch (e) { /* login redirect ho chuka hoga */ }
})();
