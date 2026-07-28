const api = '/api';
const money = value => new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:0}).format(value);
const esc = text => String(text ?? '').replace(/[&<>'"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));

async function loadDashboard(){
  const [dashboard,campaigns,expenses] = await Promise.all([fetch(`${api}/dashboard`).then(r=>r.json()),fetch(`${api}/campaigns`).then(r=>r.json()),fetch(`${api}/expenses`).then(r=>r.json())]);
  ['totalRaised','totalSpent','available','totalGoal'].forEach(k=>document.getElementById(k).textContent=money(dashboard[k]));
  document.getElementById('heroRaised').textContent=money(dashboard.totalRaised);
  document.getElementById('campaignList').innerHTML=campaigns.map(c=>{const percent=Math.min(100,Math.round(c.raised/c.goal*100)||0);return `<article class="campaign"><div class="campaign-head"><span>${esc(c.name)}</span><span>${percent}%</span></div><small>${esc(c.description)}</small><div class="bar"><i style="width:${percent}%"></i></div><div class="campaign-values"><span>${money(c.raised)} raised</span><span>Goal: ${money(c.goal)} · Spent: ${money(c.spent)}</span></div></article>`}).join('');
  const max=Math.max(...dashboard.categories.map(c=>Number(c.amount)),1); document.getElementById('categoryList').innerHTML=dashboard.categories.map(c=>`<div class="category-row"><div><span>${esc(c.category)}</span><b>${money(c.amount)}</b></div><i style="width:${Math.round(c.amount/max*100)}%"></i></div>`).join('');
  document.getElementById('expenseTable').innerHTML=expenses.map(e=>`<tr><td>${esc(e.campaign)}</td><td>${esc(e.item)}</td><td>${esc(e.category)}</td><td>${e.spentOn}</td><td>${money(e.amount)}</td></tr>`).join('');
  document.querySelectorAll('.campaign-select').forEach(select=>select.innerHTML=campaigns.map(c=>`<option value="${c.id}">${esc(c.name)}</option>`).join(''));
}

document.querySelectorAll('.tab').forEach(tab=>tab.addEventListener('click',()=>{document.querySelectorAll('.tab').forEach(t=>t.classList.remove('active'));document.querySelectorAll('.entry-form').forEach(f=>f.classList.remove('active'));tab.classList.add('active');document.getElementById(tab.dataset.form).classList.add('active');}));
document.querySelectorAll('.entry-form').forEach(form=>form.addEventListener('submit',async e=>{e.preventDefault();const data=Object.fromEntries(new FormData(form));Object.keys(data).forEach(k=>{if(['goal','amount','campaignId'].includes(k))data[k]=Number(data[k])});if(!data.donorName)data.donorName='Anonymous';const endpoint=form.id.replace('Form','s');try{const res=await fetch(`${api}/${endpoint}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(data)});if(!res.ok)throw new Error();form.reset();document.getElementById('message').textContent='Saved successfully. The dashboard has been refreshed.';await loadDashboard();}catch{document.getElementById('message').textContent='Could not save. Check that the Java server and MySQL are running.'}}));
document.querySelectorAll('input[type="date"]').forEach(i=>i.value=new Date().toISOString().slice(0,10));
loadDashboard().catch(()=>document.getElementById('message').textContent='Could not load data. Start MySQL and then the Java application.');
