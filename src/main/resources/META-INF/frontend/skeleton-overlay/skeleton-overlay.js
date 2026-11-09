/**
 * <skeleton-overlay> covers its parent element and draws a placeholder block on top
 * of every child it finds, matching each child's position, size and shape.
 * The real children are not modified apart from being made inert while loading.
 */

// Elements we descend into instead of covering as one block.
const CONTAINER_TAGS = new Set([
  'VAADIN-VERTICAL-LAYOUT', 'VAADIN-HORIZONTAL-LAYOUT', 'VAADIN-FORM-LAYOUT',
  'VAADIN-FORM-ROW', 'VAADIN-FORM-ITEM', 'VAADIN-SCROLLER', 'VAADIN-CARD',
  'DIV', 'SECTION', 'ARTICLE', 'HEADER', 'FOOTER', 'MAIN', 'NAV', 'ASIDE',
  'FORM', 'FIELDSET', 'UL', 'OL', 'LI', 'DL',
]);

const TEXT_TAGS = new Set([
  'P', 'SPAN', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6', 'LABEL', 'LI', 'DD', 'DT',
  'STRONG', 'EM', 'SMALL', 'A', 'BLOCKQUOTE', 'PRE', 'CODE', 'DIV',
]);

const CIRCLE_TAGS = new Set(['VAADIN-AVATAR', 'VAADIN-AVATAR-GROUP']);

// Grids are drawn as a header row plus body rows, one bar per cell.
const GRID_TAGS = new Set(['VAADIN-GRID', 'VAADIN-GRID-PRO']);
const GRID_CELL_PADDING = 16;
const GRID_DEFAULT_ROW_HEIGHT = 36;

const template = document.createElement('template');
template.innerHTML = `
  <style>
    :host {
      position: absolute;
      top: 0;
      left: 0;
      z-index: var(--skeleton-z-index, 10);
      background: var(--skeleton-background, var(--lumo-base-color, Canvas));
      pointer-events: auto;
      cursor: progress;
      overflow: hidden;
      box-sizing: border-box;
      /* Not a flex/grid item visually; absolute positioning takes it out of flow. */
    }
    .block {
      position: absolute;
      overflow: hidden;
      background: var(--skeleton-color, var(--lumo-contrast-10pct, rgba(128, 128, 128, 0.18)));
      border-radius: var(--skeleton-radius, 6px);
    }
    .block.circle { border-radius: 50%; }
    .block.frame {
      background: none;
      border: 1px solid var(--skeleton-color, var(--lumo-contrast-10pct, rgba(128, 128, 128, 0.18)));
      box-sizing: border-box;
    }
    .block.frame::after { display: none; }
    :host([animation="pulse"]) .block.frame { animation: none; }
    :host([animation="shimmer"]) .block::after {
      content: "";
      position: absolute;
      inset: 0;
      transform: translateX(-100%);
      background: linear-gradient(90deg, transparent,
        var(--skeleton-highlight, rgba(255, 255, 255, 0.35)), transparent);
      animation: skeleton-shimmer var(--skeleton-duration, 1.4s) ease-in-out infinite;
    }
    :host([animation="pulse"]) .block {
      animation: skeleton-pulse var(--skeleton-duration, 1.4s) ease-in-out infinite;
    }
    @keyframes skeleton-shimmer { 100% { transform: translateX(100%); } }
    @keyframes skeleton-pulse { 50% { opacity: 0.45; } }
    @media (prefers-reduced-motion: reduce) {
      .block, .block::after { animation: none !important; }
    }
    .sr-only {
      position: absolute; width: 1px; height: 1px; overflow: hidden;
      clip: rect(0 0 0 0); white-space: nowrap;
    }
  </style>
  <span class="sr-only" role="status">Loading</span>
  <div id="layer"></div>
`;

class SkeletonOverlay extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({ mode: 'open' }).appendChild(template.content.cloneNode(true));
    this._layer = this.shadowRoot.getElementById('layer');
    this._frame = 0;
    this._inerted = new Set();
    this._schedule = () => {
      if (!this._frame) {
        this._frame = requestAnimationFrame(() => {
          this._frame = 0;
          this._render();
        });
      }
    };
  }

  connectedCallback() {
    const host = this.parentElement;
    if (!host) return;
    this._host = host;

    // The overlay is positioned relative to the host, so the host must be positioned.
    if (getComputedStyle(host).position === 'static') {
      this._restorePosition = host.style.position;
      host.style.position = 'relative';
    }
    this._previousBusy = host.getAttribute('aria-busy');
    host.setAttribute('aria-busy', 'true');

    this._resizeObserver = new ResizeObserver(this._schedule);
    this._resizeObserver.observe(host);
    this._mutationObserver = new MutationObserver((records) => {
      // Ignore our own changes to the host (style/aria-busy) to avoid loops.
      if (records.some((r) => r.target !== this && !this.contains(r.target))) {
        this._schedule();
      }
    });
    this._mutationObserver.observe(host, {
      childList: true, subtree: true, characterData: true, attributes: true,
      attributeFilter: ['class', 'style', 'hidden', 'theme'],
    });
    window.addEventListener('resize', this._schedule);
    this._schedule();
  }

  disconnectedCallback() {
    cancelAnimationFrame(this._frame);
    this._frame = 0;
    this._resizeObserver?.disconnect();
    this._mutationObserver?.disconnect();
    window.removeEventListener('resize', this._schedule);
    for (const el of this._inerted) el.inert = false;
    this._inerted.clear();

    const host = this._host;
    if (!host) return;
    if (this._restorePosition !== undefined) {
      host.style.position = this._restorePosition;
      this._restorePosition = undefined;
    }
    if (this._previousBusy === null) host.removeAttribute('aria-busy');
    else host.setAttribute('aria-busy', this._previousBusy);
    this._host = undefined;
  }

  _render() {
    const host = this._host;
    if (!host || !host.isConnected) return;

    // Cover the full scrollable area of the host (reset first so we never inflate it).
    this.style.width = '0px';
    this.style.height = '0px';
    this.style.width = `${host.scrollWidth}px`;
    this.style.height = `${host.scrollHeight}px`;

    // Keep real content out of the tab order and accessibility tree while loading.
    for (const child of host.children) {
      if (child !== this && !child.inert) {
        child.inert = true;
        this._inerted.add(child);
      }
    }

    const hostRect = host.getBoundingClientRect();
    const originX = hostRect.left + host.clientLeft - host.scrollLeft;
    const originY = hostRect.top + host.clientTop - host.scrollTop;

    const depth = this.getAttribute('depth') || 'leaves';
    const textLines = this.getAttribute('text-lines') !== 'false';
    const targets = [];
    for (const child of host.children) {
      if (child !== this) this._collect(child, depth === 'leaves', targets);
    }

    const fragment = document.createDocumentFragment();
    for (const el of targets) {
      for (const shape of this._shapesFor(el, textLines)) {
        const block = document.createElement('div');
        block.className = shape.circle ? 'block circle' : shape.frame ? 'block frame' : 'block';
        block.style.left = `${shape.x - originX}px`;
        block.style.top = `${shape.y - originY}px`;
        block.style.width = `${shape.w}px`;
        block.style.height = `${shape.h}px`;
        if (shape.radius) block.style.borderRadius = shape.radius;
        fragment.appendChild(block);
      }
    }
    this._layer.replaceChildren(fragment);
  }

  _collect(el, descend, out) {
    if (el.hasAttribute('skeleton-ignore') || el.tagName === 'SKELETON-OVERLAY') return;
    const style = getComputedStyle(el);
    if (style.display === 'none' || style.visibility === 'hidden') return;

    // display: contents has no box of its own; look at its children instead.
    if (style.display === 'contents') {
      for (const child of el.children) this._collect(child, descend, out);
      return;
    }

    if (descend && this._isContainer(el)) {
      for (const child of el.children) this._collect(child, descend, out);
      return;
    }
    out.push(el);
  }

  _isContainer(el) {
    if (el.hasAttribute('skeleton-leaf')) return false;
    if (!CONTAINER_TAGS.has(el.tagName) || el.children.length === 0) return false;
    // An element with its own visible text (e.g. <div>Hello <b>x</b></div>) is a leaf.
    for (const node of el.childNodes) {
      if (node.nodeType === Node.TEXT_NODE && node.textContent.trim()) return false;
    }
    return true;
  }

  _shapesFor(el, textLines) {
    if (GRID_TAGS.has(el.tagName) && !el.hasAttribute('skeleton-leaf')) {
      const shapes = this._gridShapes(el);
      if (shapes) return shapes;
    }
    const rect = el.getBoundingClientRect();
    const style = getComputedStyle(el);
    const empty = rect.width < 1 || rect.height < 1;
    const isText = TEXT_TAGS.has(el.tagName) && el.children.length === 0
      && (empty || el.textContent.trim().length > 0);
    const fontSize = parseFloat(style.fontSize) || 16;
    const lineHeight = parseFloat(style.lineHeight) || fontSize * 1.4;

    let { left: x, top: y, width: w, height: h } = rect;

    // Empty text elements (data not loaded yet) have no size: invent a sensible line.
    if (w < 1 || h < 1) {
      if (!isText) return [];
      const parentWidth = el.parentElement?.getBoundingClientRect().width || 200;
      w = w < 1 ? Math.min(parentWidth, Math.max(80, parentWidth * 0.6)) : w;
      h = h < 1 ? lineHeight : h;
    }

    const circle = CIRCLE_TAGS.has(el.tagName) || this._isRound(style, w, h);
    if (circle) {
      return [{ x, y, w, h, circle: true }];
    }

    if (isText) {
      const barHeight = Math.max(8, Math.round(fontSize * 0.9));
      const lines = Math.max(1, Math.round(h / lineHeight));
      if (!textLines || lines === 1) {
        return [{ x, y: y + (Math.min(h, lineHeight) - barHeight) / 2, w, h: barHeight }];
      }
      const shapes = [];
      for (let i = 0; i < lines; i++) {
        const lineWidth = i === lines - 1 ? w * 0.6 : w;
        shapes.push({ x, y: y + i * lineHeight + (lineHeight - barHeight) / 2, w: lineWidth, h: barHeight });
      }
      return shapes;
    }

    // Keep the component's own corner radius so buttons and fields look like themselves.
    const radius = style.borderRadius && style.borderRadius !== '0px' ? style.borderRadius : null;
    return [{ x, y, w, h, radius }];
  }

  /**
   * One bar per header cell and per visible body cell. When the grid has no rows yet,
   * placeholder rows are drawn under the header columns to fill the grid's height.
   * Returns null if the grid has not rendered its header yet (drawn as one block).
   */
  _gridShapes(grid) {
    const header = grid.shadowRoot?.getElementById('header');
    const items = grid.shadowRoot?.getElementById('items');
    if (!header || !items) return null;

    const gridRect = grid.getBoundingClientRect();
    const visible = (r) => r.width >= 1 && r.height >= 1
      && r.left < gridRect.right && r.right > gridRect.left
      && r.top < gridRect.bottom && r.bottom > gridRect.top;
    const cellsOf = (row) => [...row.children]
      .filter((cell) => !cell.hidden && !cell.getAttribute('part')?.includes('details-cell'))
      .map((cell) => cell.getBoundingClientRect())
      .filter(visible);
    const rowsOf = (section) => [...section.children]
      .filter((row) => !row.hidden)
      .map((row) => ({ rect: row.getBoundingClientRect(), cells: cellsOf(row) }))
      .filter((row) => row.cells.length > 0);

    const headerRows = rowsOf(header);
    if (headerRows.length === 0) return null;

    const shapes = [{ x: gridRect.left, y: gridRect.top, w: gridRect.width, h: gridRect.height, frame: true }];
    const bar = (cell, rowIndex, colIndex, top, height) => {
      const pad = Math.min(GRID_CELL_PADDING, cell.width / 4);
      // Vary bar widths a little so the rows don't look like a solid table.
      const fraction = 0.5 + ((rowIndex * 7 + colIndex * 3) % 5) * 0.1;
      const barHeight = Math.min(12, height / 2);
      const right = Math.min(cell.right, gridRect.right);
      shapes.push({
        x: cell.left + pad,
        y: top + (height - barHeight) / 2,
        w: Math.max(8, (right - cell.left - 2 * pad) * fraction),
        h: barHeight,
      });
    };

    headerRows.forEach((row) => row.cells.forEach((cell, c) => bar(cell, 0, c, cell.top, cell.height)));

    const bodyRows = rowsOf(items);
    if (bodyRows.length > 0) {
      bodyRows.forEach((row, r) => row.cells.forEach((cell, c) => {
        const height = Math.min(cell.bottom, gridRect.bottom) - cell.top;
        if (height >= 8) bar(cell, r + 1, c, cell.top, height);
      }));
      return shapes;
    }

    // No data yet: fake rows using the last header row's columns.
    const columns = headerRows[headerRows.length - 1].cells;
    const rowHeight = headerRows[headerRows.length - 1].rect.height || GRID_DEFAULT_ROW_HEIGHT;
    let top = headerRows[headerRows.length - 1].rect.bottom;
    for (let r = 1; top + rowHeight <= gridRect.bottom; r++, top += rowHeight) {
      columns.forEach((cell, c) => bar(cell, r, c, top, rowHeight));
    }
    return shapes;
  }

  _isRound(style, w, h) {
    const raw = style.borderTopLeftRadius;
    if (!raw || Math.abs(w - h) > 2) return false;
    const value = parseFloat(raw);
    return raw.endsWith('%') ? value >= 50 : value >= Math.min(w, h) / 2 - 1;
  }
}

if (!customElements.get('skeleton-overlay')) {
  customElements.define('skeleton-overlay', SkeletonOverlay);
}
