'use strict';

function renderClassDetail(fqn) {
  const cls    = API.classes[fqn];
  const simple = fqn.split('.').pop();
  const panel  = document.getElementById('detail-panel');

  panel.innerHTML = `
    <div class="class-header">
      <h2>${esc(simple)}</h2>
      <div class="fqn">${esc(fqn)}</div>
      <div class="badges">
        <span class="badge ${cls.set_exposed ? 'badge-exposed' : 'badge-no'}">${cls.set_exposed ? 'setExposed' : 'not setExposed'}</span>
        <span class="badge ${cls.lua_tagged  ? 'badge-tagged'  : 'badge-no'}">${cls.lua_tagged  ? '@UsedFromLua' : 'not @UsedFromLua'}</span>
        ${cls.is_enum ? '<span class="badge badge-enum">enum</span>' : ''}
      </div>
      <div class="source-path">${esc(cls.source_file)}</div>
    </div>
    ${(cls.constructors || []).length ? `
    <div class="section">
      <div class="section-header">
        <h3>Constructors</h3><span class="count">${(cls.constructors || []).length}</span>
      </div>
      <div id="constructors-wrap">${renderConstructorsTable(cls)}</div>
    </div>` : ''}
    <div class="section">
      <div class="section-header">
        <h3>Methods</h3><span class="count" id="method-count">${cls.methods.length}</span>
        ${cls.methods.length ? `<input class="inline-search" id="method-search-inp" type="text" placeholder="Filter…" value="${esc(methodSearch)}" autocomplete="off">` : ''}
      </div>
      <div id="methods-wrap"></div>
    </div>
    <div class="section">
      <div class="section-header">
        <h3>${cls.is_enum ? 'Constants' : 'Fields'}</h3><span class="count" id="field-count">${cls.fields.length}</span>
        ${cls.fields.length && !cls.is_enum ? `<input class="inline-search" id="field-search-inp" type="text" placeholder="Filter…" value="${esc(fieldSearch)}" autocomplete="off">` : ''}
      </div>
      <div id="fields-wrap"></div>
    </div>`;

  switchCtab('detail');
  refreshMethods(cls);
  refreshFields(cls);

  const mi = document.getElementById('method-search-inp');
  if (mi) { mi.addEventListener('input', () => { methodSearch = mi.value; refreshMethods(cls); }); if (methodSearch) mi.focus(); }
  const fi = document.getElementById('field-search-inp');
  if (fi) { fi.addEventListener('input', () => { fieldSearch  = fi.value; refreshFields(cls);  }); if (fieldSearch && !methodSearch) fi.focus(); }
}

function refreshMethods(cls) {
  const s      = methodSearch.toLowerCase();
  const tagged = cls.methods.filter(m =>  m.lua_tagged && (!s || m.name.toLowerCase().includes(s)));
  const other  = cls.methods.filter(m => !m.lua_tagged && (!s || m.name.toLowerCase().includes(s)));
  document.getElementById('method-count').textContent = tagged.length + other.length;
  document.getElementById('methods-wrap').innerHTML   = renderMethodGroups(tagged, other, cls.set_exposed);
}

function refreshFields(cls) {
  const s      = fieldSearch.toLowerCase();
  const fields = cls.fields.filter(f => !s || f.name.toLowerCase().includes(s) || f.type.toLowerCase().includes(s));
  document.getElementById('field-count').textContent = fields.length;
  document.getElementById('fields-wrap').innerHTML   = renderFieldsTable(fields, cls.is_enum);
}

function renderConstructorsTable(cls) {
  const ctors = cls.constructors || [];
  if (!ctors.length) return '<div class="empty-msg">No public constructors</div>';
  const simple = cls.simple_name;
  const rows = ctors.map(c => {
    const dot = c.lua_tagged
      ? `<span class="dot dot-tagged" style="display:inline-block;margin-left:5px;vertical-align:middle" title="@UsedFromLua"></span>`
      : '';
    return `<tr>
      <td><a class="method-link" data-method="${esc(simple)}" data-is-ctor="1">${esc(simple)}</a>${dot}</td>
      <td><span class="params-cell">(${renderParams(c.params)})</span></td>
    </tr>`;
  }).join('');
  return `<table><thead><tr><th>Constructor</th><th>Parameters</th></tr></thead><tbody>${rows}</tbody></table>`;
}

function renderMethodGroups(tagged, other, setExposed) {
  if (!tagged.length && !other.length) return `<div class="empty-msg">No methods</div>`;
  // Split each group into instance vs static
  const taggedInst   = tagged.filter(m => !m.static);
  const taggedStatic = tagged.filter(m =>  m.static);
  const otherInst    = other.filter(m => !m.static);
  const otherStatic  = other.filter(m =>  m.static);
  let html = '';
  let topMargin = false;
  if (taggedInst.length) {
    html += `<div class="group-label tagged-label"><span class="dot dot-tagged"></span> @UsedFromLua (${taggedInst.length})</div>` + renderMethodsTable(taggedInst);
    topMargin = true;
  }
  if (taggedStatic.length) {
    html += `<div class="group-label tagged-label" style="margin-top:${topMargin ? 8 : 0}px"><span class="dot dot-tagged"></span> @UsedFromLua — static (${taggedStatic.length})</div>` + renderMethodsTable(taggedStatic);
    topMargin = true;
  }
  if (otherInst.length) {
    const lbl = setExposed ? `setExposed — callable (${otherInst.length})` : `Not setExposed (${otherInst.length})`;
    html += `<div class="group-label other-label" style="margin-top:${topMargin ? 12 : 0}px"><span class="dot dot-empty"></span> ${lbl}</div>` + renderMethodsTable(otherInst);
    topMargin = true;
  }
  if (otherStatic.length) {
    const lbl = setExposed ? `setExposed — static, not Lua-callable (${otherStatic.length})` : `Not setExposed — static (${otherStatic.length})`;
    html += `<div class="group-label other-label" style="margin-top:${topMargin ? 8 : 0}px"><span class="dot dot-empty"></span> ${lbl}</div>` + renderMethodsTable(otherStatic);
  }
  return html;
}

function renderMethodsTable(methods) {
  if (!methods.length) return '';
  const rows = methods.map(m => {
    const staticTag = m.static ? `<span class="tag-static" style="margin-left:5px">static</span>` : '';
    return `<tr>
      <td><a class="method-link" data-method="${esc(m.name)}">${esc(m.name)}</a>${staticTag}</td>
      <td><span class="return-type">${esc(m.return_type)}</span></td>
      <td><span class="params-cell">${renderParams(m.params) || '<span style="color:#444">—</span>'}</span></td>
    </tr>`;
  }).join('');
  return `<table><thead><tr><th>Method</th><th>Returns</th><th>Parameters</th></tr></thead><tbody>${rows}</tbody></table>`;
}

function renderFieldsTable(fields, isEnum) {
  if (!fields.length) return `<div class="empty-msg">No fields</div>`;
  if (isEnum) {
    return `<div style="padding:8px 0;line-height:2.2">${fields.map(f => `<span class="field-name" style="margin-right:12px">${esc(f.name)}</span>`).join('')}</div>`;
  }
  return `<table><thead><tr><th>Field</th><th>Type</th></tr></thead><tbody>${
    fields.map(f => {
      const dot       = f.lua_tagged ? `<span class="dot dot-tagged" style="display:inline-block;margin-left:5px;vertical-align:middle" title="@UsedFromLua"></span>` : '';
      const staticTag = f.static     ? `<span class="tag-static" style="margin-left:5px">static</span>` : '';
      return `<tr><td><span class="field-name">${esc(f.name)}</span>${dot}${staticTag}</td><td><span class="field-type">${esc(f.type)}</span></td></tr>`;
    }).join('')
  }</tbody></table>`;
}
