import{r as e}from"./B0Z9INg1.js";import{Ht as t,J as n,b as r,ct as i,ft as a,g as o,k as s,mt as c,tt as l,vn as u}from"./DAji9eeE.js";import{A as d,B as f,C as p,D as m,E as h,F as g,G as _,H as v,O as y,R as b,a as x,b as S,c as C,d as w,j as T,l as E,o as D,s as O,u as k,w as A,x as j,y as M,z as N}from"./CcV2bqHg.js";var P={...S,curveType:E.MonotoneX,lineWidth:2,lineDashArray:void 0,fallbackValue:void 0,highlightOnHover:!1,cursor:null,interpolateMissingData:!1},F=e({dim:()=>V,globalStyles:()=>I,interpolatedPath:()=>H,line:()=>R,linePath:()=>z,lineSelectionHelper:()=>B,root:()=>L}),I=T`
  :root {
    --vis-line-cursor: default;
    --vis-line-stroke-dasharray: none;
    --vis-line-stroke-dashoffset: 0;

    --vis-line-gapfill-stroke-dasharray: 2 3;
    --vis-line-gapfill-stroke-opacity: 0.8;
    --vis-line-gapfill-stroke-dashoffset: 0;
  }
`,L=d`
  label: line-component;
`,R=d`
  label: line;
  transition: opacity 200ms;
  cursor: var(--vis-line-cursor);
`,z=d`
  label: linePath;
  fill: none;
  stroke-dasharray: var(--vis-line-stroke-dasharray);
  stroke-dashoffset: var(--vis-line-stroke-dashoffset);
`,B=d`
  label: lineSelectionHelper;
  fill: none;
  stroke: rgba(0, 0, 0, 0);
  stroke-width: 8px;
`,V=d`
  opacity: 0.2;
`,H=d`
  label: interpolated-path;
  fill: none;
  stroke-dasharray: var(--vis-line-gapfill-stroke-dasharray);
  stroke-dashoffset: var(--vis-line-gapfill-stroke-dashoffset);
  stroke-opacity: var(--vis-line-gapfill-stroke-opacity);
`,U=class e extends M{constructor(t){super(),this._defaultConfig=P,this.config=this._defaultConfig,this.curve=C[E.MonotoneX],this.events={[e.selectors.line]:{mouseover:this._highlight.bind(this),mouseleave:this._resetHighlight.bind(this)}},this.setConfig(t)}get bleed(){let{config:{lineWidth:e}}=this,t=this.yScale.domain(),n=(this.yScale.range()[0]>this.yScale.range()[1]?j.North:j.South)===j.South,r=e>3,i=e>=10;return{top:!i&&(!n&&t[1]===0||n&&t[0]===0)?0:e/2,bottom:!i&&(!n&&t[0]===0||n&&t[1]===0)?0:e/2,left:r?e/2:0,right:r?e/2:0}}_render(e){super._render(e);let{config:t,datamodel:{data:n}}=this,r=v(e)?e:t.duration,i={colorFn:this._colorFunction};this.curve=C[t.curveType],this.lineGen=w().x(e=>e.x).y(e=>e.y).defined(e=>e.defined).curve(this.curve);let a=f(t.y)?t.y:[t.y],o=n.map((e,n)=>this.xScale(g(e,t.x,n))),s=a.map(e=>{let r=n.map((n,r)=>{let i=g(n,e,r),a=(v(i)||i===null)&&isFinite(i)?i:t.fallbackValue,s=t.interpolateMissingData?(v(i)||i===null)&&isFinite(i):isFinite(a);return{x:o[r],y:this.yScale(a??0),defined:s,value:a}}),i=r.reduce((e,t)=>t.defined||e,!1),a=!1;return{values:r,defined:i,gaps:r.reduce((e,n,i)=>{!n.defined&&isFinite(t.fallbackValue)&&e.push({...n,defined:!0}),!n.defined&&!a&&(a=!0);let o=i>0&&!r[i-1].defined||i<r.length-1&&!r[i+1].defined;return n.defined&&o&&(a||e.push({...n,defined:!1}),e.push(n),a=!1),e},[]),visible:i&&r.some(e=>e.value!==null)}}),c=this.g.selectAll(`.${R}`).data(s),l=c.enter().append(`g`).attr(`class`,R);l.append(`path`).attr(`class`,z).attr(A,(e,t)=>t).attr(`stroke`,(e,r)=>y(n,t.color,r,t.colorKeys?.[r],i)).attr(`stroke-opacity`,0).attr(`stroke-width`,t.lineWidth),l.append(`path`).attr(`class`,B).attr(`d`,this._emptyPath()),l.append(`path`).attr(`class`,H).attr(`d`,this._emptyPath()).style(`opacity`,0);let u=l.merge(c);u.style(`cursor`,(e,r)=>b(n,t.cursor,r)),u.each((e,a,o)=>{let s=_(o[a]),c=s.select(`.${z}`),l=s.select(`.${B}`),u=s.select(`.${H}`),d=e.visible,f=y(n,t.color,a,t.colorKeys?.[a],i),g=h(m(n,t.pattern,a),f,c.node()),v=N(n,t.lineDashArray,a)?.join(` `)??g?.dashArray??null;c.style(`marker`,g?.marker??null);let b=p(c,r).attr(`stroke`,f).attr(`stroke-width`,t.lineWidth).attr(`stroke-opacity`,+!!d).style(`stroke-dasharray`,v),x=e.values.some(e=>!e.defined),S=this.lineGen(e.values);if(r&&!x){let e=c.attr(`d`)||this._emptyPath(),t=S||this._emptyPath();b.attrTween(`d`,()=>k(e,t))}else e.visible&&b.attr(`d`,S);l.attr(`d`,S).attr(`visibility`,d?null:`hidden`),x&&t.interpolateMissingData?p(u,r).attr(`d`,this.lineGen(e.gaps)).attr(`stroke`,y(n,t.color,a,t.colorKeys?.[a],i)).attr(`stroke-width`,t.lineWidth-1).style(`opacity`,1):u.transition().duration(r).style(`opacity`,0)}),p(c.exit(),r).style(`opacity`,0).remove()}_emptyPath(){let e=this.xScale.range(),t=this.yScale.range();return`M${e[0]},${t[0]} L${e[1]},${t[0]}`}_highlight(e){let{config:t}=this;t.highlightOnHover&&this.g.selectAll(`.${R}`).classed(V,t=>t!==e)}_resetHighlight(){let{config:e}=this;e.highlightOnHover&&this.g.selectAll(`.${R}`).classed(V,!1)}};U.selectors=F;var W=U,G={"data-vis-component":``};W.selectors;var K=s({__name:`index`,props:{color:{type:[Function,String,Array,null]},pattern:{type:[Function,null]},curveType:{},lineWidth:{},lineDashArray:{type:[Function,Array,null]},fallbackValue:{},highlightOnHover:{type:Boolean},cursor:{type:[Function,String,null]},interpolateMissingData:{type:Boolean},x:{type:[Function,Number,null]},y:{type:[Function,Number,null,Array]},id:{type:Function},colorKeys:{},xScale:{type:[Object,Function]},yScale:{type:[Object,Function]},excludeFromDomainCalculation:{type:Boolean},duration:{},events:{},attributes:{},data:{}},setup(e,{expose:s}){let d=e,f=n(O),p=o(()=>f.data.value??d.data),m=x(d),h=u();return i(()=>{l(()=>{h.value=new W(m.value),h.value?.setData(p.value),f.update(h.value)})}),a(()=>{h.value?.destroy(),f.destroy()}),t(m,(e,t)=>{D(e,t)||(h.value?.setConfig(m.value),h.value?.render())}),t(p,()=>{h.value?.setData(p.value)}),s({component:h}),(e,t)=>(c(),r(`div`,G))}});export{K as t};