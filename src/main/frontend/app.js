(function(){
  /* ====== конфиг / состояние ====== */
  var API   = window.SQUARES_API || "http://127.0.0.1:3000/api/squares/nextMove";

  var CELL  = 56;                       
  var DPR   = Math.max(1, Math.floor(window.devicePixelRatio || 1));

  // state.winQuad — массив из 4 углов победного квадрата [{x,y},...]
  var state = null; // { n, board, turn, you, history[], finished, winner, winQuad }
  var busy  = false; // ждём ответ API

  var cv   = document.getElementById('board');
  var ctx  = cv.getContext('2d');
  ctx.imageSmoothingEnabled = true;

  var movesEl  = document.getElementById('moves');
  var statusEl = document.getElementById('status');
  var apiLbl   = document.getElementById('apiLabel'); if (apiLbl) apiLbl.textContent = API;
  var wrapEl   = cv.parentElement;

  document.getElementById('newBtn').onclick  = function(){ newGame(); };
  document.getElementById('undoBtn').onclick = function(){ undo(); };
  document.getElementById('fastBtn').onclick = function(){ autoMove(); };
  document.getElementById('resetBtn').onclick= function(){ newGame(); };

  cv.addEventListener('click', onCanvasClick);
  window.addEventListener('resize', function(){ if(state) { resizeCanvas(state.n); renderAll(); }});

  newGame();
  requestAnimationFrame(function(){ if(state){ resizeCanvas(state.n); renderAll(); }});

  /* ====== инициализация ====== */
  function newGame(){
    var n   = clamp(+document.getElementById('sizeInp').value || 11, 3, 50);
    var you = document.getElementById('colorSel').value;

    // всегда первым ходит белый
    state = {
      n:n, board:mk2D(n), turn:'W', you:you,
      history:[], finished:false, winner:null, winQuad:null
    };
    movesEl.innerHTML = "";
    resizeCanvas(n);
    setStatus("Ваш цвет: " + (you==='W'?'White':'Black') + ". Ходит " + who(state.turn));
    renderAll();

    if (state.you === 'B') autoMove(); 
  }

  function resizeCanvas(n){
    var pad = 32; 
    var w = Math.max(200, wrapEl.clientWidth  - pad);
    var h = Math.max(200, wrapEl.clientHeight - pad);

    var side = Math.floor(Math.min(w, h));
    var cell = Math.max(24, Math.floor(side / n)); 
    CELL = cell;

    var W = CELL*n, H = CELL*n;
    cv.width = W*DPR; cv.height = H*DPR;
    cv.style.width = W+'px'; cv.style.height = H+'px';
    ctx.setTransform(DPR,0,0,DPR,0,0);
  }

  function mk2D(n){ var a=[],y,x; for(y=0;y<n;y++){ a[y]=[]; for(x=0;x<n;x++) a[y][x]=null; } return a; }

  /* ====== взаимодействие ====== */
  function onCanvasClick(e){
    if (busy || state.finished) return;
    var r = cv.getBoundingClientRect();
    var x = Math.floor((e.clientX - r.left) / CELL);
    var y = Math.floor((e.clientY - r.top)  / CELL);
    if (x<0 || y<0 || x>=state.n || y>=state.n) return;
    if (state.turn !== state.you) return;
    if (state.board[y][x] != null) return;

    place(x,y,state.you);
    logMove(state.you, x, y);
    if (checkFinish()) return;
    autoMove();
  }

  function place(x,y,color){
    state.board[y][x]=color;
    state.history.push({x:x,y:y,c:color});
    state.turn = opp(color);
    setStatus("Ходит " + who(state.turn)); 
    renderAll();
  }

  function undo(){
    if (busy) return;
    if (!state.history.length) return;
    if (state.finished){ state.finished=false; state.winner=null; state.winQuad=null; }

    var steps = (state.turn === state.you) ? 2 : 1;
    while(steps-- && state.history.length){
      var m = state.history.pop();
      state.board[m.y][m.x]=null;
      state.turn = m.c;
      if (movesEl.lastElementChild) movesEl.removeChild(movesEl.lastElementChild);
    }
    setStatus("Ходит " + who(state.turn));
    renderAll();
  }

  function autoMove(){
    if (state.finished) return;
    if (state.turn === state.you) return; 
    if (busy) return;
    busy = true;

    var dto = { size: state.n, data: boardToString(state.board), nextPlayerColor: (state.turn==='W'?'w':'b') };
    var aiColor = state.turn; 

    xhrJSON('POST', API, dto, function(err, resp, code){
      busy = false;
      if (state.finished) return; 
      if (err) { setStatus("API error: "+err); return; }
      if (code === 204 || !resp) { 
        state.finished = true; state.winner = null; state.winQuad=null;
        renderAll(); 
        return;
      }

      place(resp.x, resp.y, aiColor);
      logMove(aiColor, resp.x, resp.y);
      checkFinish();
    });
  }

  /* ====== рендер ====== */
  function renderAll(){
    drawBoard();
    for (var y=0;y<state.n;y++)
      for (var x=0;x<state.n;x++)
        if (state.board[y][x]) drawChip(x,y,state.board[y][x]);

    // подсветка выигрышного квадрата (если есть)
    if (state.finished && state.winQuad){
      drawWinOverlay(state.winQuad, state.winner || 'W');
    }

    if (state.finished){
      ctx.save();
      var W = state.n*CELL, H = state.n*CELL;
      ctx.fillStyle = 'rgba(0,0,0,.25)';
      ctx.fillRect(0, H-36, W, 36);
      ctx.fillStyle = 'rgba(255,255,255,.85)';
      ctx.font = '600 16px ui-sans-serif, system-ui, -apple-system, Segoe UI';
      ctx.textAlign = 'center';
      ctx.fillText(finishText(), W/2, H-13);
      ctx.restore();
    }
  }

  function drawBoard(){
    var n = state.n, W = n*CELL, H = n*CELL;

    var g = ctx.createLinearGradient(0,0,0,H);
    g.addColorStop(0, '#171c25'); g.addColorStop(1, '#0f141b');
    ctx.fillStyle = g;
    ctx.fillRect(0,0,W,H);

    for (var y=0;y<n;y++){
      for (var x=0;x<n;x++){
        if ((x+y)%2==1){
          ctx.fillStyle = 'rgba(255,255,255,.02)';
          ctx.fillRect(x*CELL, y*CELL, CELL, CELL);
        }
      }
    }

    // сетка — «тихий неон»
    ctx.lineWidth = 1;
    for (var i=0;i<=n;i++){
      var x = i*CELL + .5, y = i*CELL + .5;
      ctx.strokeStyle = 'rgba(88,208,255,.22)'; // бирюза
      ctx.beginPath(); ctx.moveTo(x,0); ctx.lineTo(x,H); ctx.stroke();
      ctx.strokeStyle = 'rgba(255,116,168,.22)'; // розовый
      ctx.beginPath(); ctx.moveTo(0,y); ctx.lineTo(W,y); ctx.stroke();
    }

    // рамка
    ctx.strokeStyle = 'rgba(255,255,255,.12)';
    ctx.lineWidth = 2;
    ctx.strokeRect(0.5,0.5,W-1,H-1);
  }

  // фишка: мягкий чип с приглушённым свечением
  function drawChip(gx,gy,c){
    var cx = gx*CELL + CELL/2;
    var cy = gy*CELL + CELL/2;
    var r  = Math.max(10, CELL*0.32);

    // свечение
    ctx.save();
    ctx.globalCompositeOperation = 'screen';
    ctx.shadowBlur   = 30;
    ctx.shadowColor  = (c==='W')? 'rgba(88,208,255,.25)' : 'rgba(255,116,168,.25)';
    ctx.fillStyle    = 'rgba(255,255,255,.02)';
    ctx.beginPath(); ctx.arc(cx,cy,r+1,0,Math.PI*2); ctx.fill();
    ctx.restore();

    // тень
    ctx.save();
    ctx.shadowColor  = 'rgba(0,0,0,.45)';
    ctx.shadowBlur   = 18;
    ctx.shadowOffsetX= 6;
    ctx.shadowOffsetY= 8;

    // тело
    var g = ctx.createRadialGradient(cx - r*0.3, cy - r*0.35, r*0.3, cx, cy, r*1.05);
    if (c==='W'){ g.addColorStop(0,'#e8eef6'); g.addColorStop(1,'#aeb9c8'); }
    else       { g.addColorStop(0,'#96a0ab'); g.addColorStop(1,'#56606b'); }
    ctx.fillStyle = g;
    ctx.beginPath(); ctx.arc(cx,cy,r,0,Math.PI*2); ctx.fill();
    ctx.restore();

    // кромка и блик
    ctx.lineWidth = 2;
    ctx.strokeStyle = 'rgba(255,255,255,.10)';
    ctx.beginPath(); ctx.arc(cx,cy,r-1,0,Math.PI*2); ctx.stroke();

    ctx.beginPath();
    ctx.strokeStyle = 'rgba(255,255,255,.35)';
    ctx.arc(cx - r*0.25, cy - r*0.35, r*0.55, -2.6, -0.6);
    ctx.stroke();
  }

  /* ====== подсветка победного квадрата ====== */
  function drawWinOverlay(win, color){
    if (!win || win.length!==4) return;
    var glow = (color==='W') ? 'rgba(88,208,255,.75)' : 'rgba(255,116,168,.75)';
    var soft = (color==='W') ? 'rgba(88,208,255,.18)' : 'rgba(255,116,168,.18)';

    function pt(p){ return { X:(p.x+0.5)*CELL, Y:(p.y+0.5)*CELL }; }
    var p0 = pt(win[0]), p1 = pt(win[1]), p2 = pt(win[2]), p3 = pt(win[3]);

    ctx.save();

    ctx.fillStyle = soft;
    ctx.beginPath();
    ctx.moveTo(p0.X,p0.Y); ctx.lineTo(p1.X,p1.Y); ctx.lineTo(p2.X,p2.Y); ctx.lineTo(p3.X,p3.Y); ctx.closePath();
    ctx.fill();

    ctx.lineWidth = 3;
    ctx.strokeStyle = glow;
    ctx.shadowColor = glow;
    ctx.shadowBlur = 16;
    ctx.beginPath();
    ctx.moveTo(p0.X,p0.Y); ctx.lineTo(p1.X,p1.Y); ctx.lineTo(p2.X,p2.Y); ctx.lineTo(p3.X,p3.Y); ctx.closePath();
    ctx.stroke();

    ctx.shadowBlur = 0;
    ctx.fillStyle = glow;
    [p0,p1,p2,p3].forEach(function(pp){
      ctx.beginPath(); ctx.arc(pp.X, pp.Y, Math.max(3, CELL*0.08), 0, Math.PI*2); ctx.fill();
    });

    ctx.restore();
  }

  /* ====== конец игры ====== */
  function checkFinish(){
    // ищем квадраты и забираем их вершины
    var wQ = findSquare(state.board, 'W');
    var bQ = findSquare(state.board, 'B');

    if (wQ || bQ){
      state.finished = true;
      if (wQ && !bQ){ state.winner='W'; state.winQuad = wQ; }
      else if (bQ && !wQ){ state.winner='B'; state.winQuad = bQ; }
      else { state.winner=null; state.winQuad = wQ || bQ; } 
      renderAll(); 
      return true;
    }

    // ничья — всё занято и ходов больше нет
    var full = true;
    outer: for (var y=0;y<state.n;y++) for (var x=0;x<state.n;x++) if (state.board[y][x]==null){ full=false; break outer; }
    if (full){
      state.finished = true; state.winner = null; state.winQuad = null;
      renderAll();
      return true;
    }
    return false;
  }

  function finishText(){
    if (state.winner === 'W') return "Game finished. White wins!";
    if (state.winner === 'B') return "Game finished. Black wins!";
    return "Game finished. Draw";
  }

  // возвращает массив из 4 углов квадрата (в порядке обхода) либо null
  function findSquare(board, color){
    var n = board.length;
    var pts = [], set = new Set();
    for (var y=0;y<n;y++) for (var x=0;x<n;x++){
      if (board[y][x] === color){ pts.push([x,y]); set.add(y+"#"+x); }
    }
    if (pts.length < 4) return null;

    for (var i=0;i<pts.length;i++){
      var ax = pts[i][0], ay = pts[i][1];
      for (var j=i+1;j<pts.length;j++){
        var bx = pts[j][0], by = pts[j][1];
        var vx = bx - ax, vy = by - ay;
        if (vx===0 && vy===0) continue;

        // вариант 1: поворот ребра на +90°
        var cx1 = ax - vy, cy1 = ay + vx;
        var dx1 = bx - vy, dy1 = by + vx;
        if (inside(cx1,cy1,n) && inside(dx1,dy1,n) && set.has(cy1+"#"+cx1) && set.has(dy1+"#"+dx1)){
          return [{x:ax,y:ay},{x:bx,y:by},{x:dx1,y:dy1},{x:cx1,y:cy1}];
        }

        // вариант 2: поворот на -90°
        var cx2 = ax + vy, cy2 = ay - vx;
        var dx2 = bx + vy, dy2 = by - vx;
        if (inside(cx2,cy2,n) && inside(dx2,dy2,n) && set.has(cy2+"#"+cx2) && set.has(dy2+"#"+dx2)){
          return [{x:ax,y:ay},{x:bx,y:by},{x:dx2,y:dy2},{x:cx2,y:cy2}];
        }
      }
    }
    return null;
  }

  function inside(x,y,n){ return x>=0 && y>=0 && x<n && y<n; }

  /* ====== util ====== */
  function boardToString(b){
    var n=b.length, out='',y,x,v;
    for(y=0;y<n;y++) for(x=0;x<n;x++){ v=b[y][x]; out += v? (v==='W'?'w':'b') : '.'; }
    return out;
  }
  function logMove(c,x,y){
    var d=document.createElement('div');
    d.textContent=(c==='W'?'W':'B')+" ("+x+", "+y+")";
    movesEl.appendChild(d); movesEl.scrollTop=1e6;
  }
  // если игра завершена — очищаем DOM-статус, чтобы не дублировать баннер на холсте
  function setStatus(t){
    if (state && state.finished) { statusEl.textContent = ""; return; }
    statusEl.textContent = t;
  }
  function clamp(v,a,b){ return Math.max(a, Math.min(b, v)); }
  function opp(c){ return c==='W'?'B':'W'; }
  function who(c){ return c==='W'?'White':'Black'; }

  function xhrJSON(method, url, obj, cb){
    try{
      var xhr = new XMLHttpRequest();
      xhr.open(method, url, true);
      xhr.setRequestHeader('Content-Type','application/json');
      xhr.onreadystatechange = function(){
        if (xhr.readyState !== 4) return;
        if (xhr.status === 200){
          try { cb(null, JSON.parse(xhr.responseText), 200); }
          catch(e){ cb('bad json'); }
        } else if (xhr.status === 204){
          cb(null, null, 204);
        } else cb('HTTP '+xhr.status);
      };
      xhr.send(JSON.stringify(obj));
    }catch(e){ cb(e.message||String(e)); }
  }
})();
