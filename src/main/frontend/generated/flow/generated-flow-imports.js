import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';

import { css, unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';
import $cssFromFile_0 from 'Frontend/aicon-styles.css?inline';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import '@vaadin/polymer-legacy-adapter/style-modules.js';
import '@vaadin/combo-box/src/vaadin-combo-box.js';
import 'Frontend/generated/jar-resources/comboBoxConnector.js';
import '@vaadin/side-nav/src/vaadin-side-nav.js';
import 'Frontend/generated/jar-resources/vaadin-grid-flow-selection-column.js';
import '@vaadin/integer-field/src/vaadin-integer-field.js';
import '@vaadin/radio-group/src/vaadin-radio-group.js';
import '@vaadin/radio-group/src/vaadin-radio-button.js';
import '@vaadin/app-layout/src/vaadin-app-layout.js';
import '@vaadin/tooltip/src/vaadin-tooltip.js';
import '@vaadin/tabs/src/vaadin-tab.js';
import '@vaadin/icon/src/vaadin-icon.js';
import '@vaadin/upload/src/vaadin-upload.js';
import '@vaadin/side-nav/src/vaadin-side-nav-item.js';
import '@vaadin/context-menu/src/vaadin-context-menu.js';
import 'Frontend/generated/jar-resources/contextMenuConnector.js';
import 'Frontend/generated/jar-resources/contextMenuTargetConnector.js';
import '@vaadin/form-layout/src/vaadin-form-item.js';
import '@vaadin/multi-select-combo-box/src/vaadin-multi-select-combo-box.js';
import '@vaadin/grid/src/vaadin-grid.js';
import '@vaadin/grid/src/vaadin-grid-column.js';
import '@vaadin/grid/src/vaadin-grid-sorter.js';
import '@vaadin/checkbox/src/vaadin-checkbox.js';
import 'Frontend/generated/jar-resources/gridConnector.ts';
import '@vaadin/button/src/vaadin-button.js';
import '@vaadin/checkbox-group/src/vaadin-checkbox-group.js';
import '@vaadin/number-field/src/vaadin-number-field.js';
import '@vaadin/text-field/src/vaadin-text-field.js';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/form-layout/src/vaadin-form-layout.js';
import '@vaadin/text-area/src/vaadin-text-area.js';
import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/app-layout/src/vaadin-drawer-toggle.js';
import '@vaadin/horizontal-layout/src/vaadin-horizontal-layout.js';
import '@vaadin/tabs/src/vaadin-tabs.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/scroller/src/vaadin-scroller.js';
import '@vaadin/grid/src/vaadin-grid-column-group.js';
import 'Frontend/generated/jar-resources/lit-renderer.ts';
import '@vaadin/notification/src/vaadin-notification.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import '@vaadin/vaadin-lumo-styles/color-global.js';
import '@vaadin/vaadin-lumo-styles/typography-global.js';
import '@vaadin/vaadin-lumo-styles/sizing.js';
import '@vaadin/vaadin-lumo-styles/spacing.js';
import '@vaadin/vaadin-lumo-styles/style.js';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';
const $css_0 = typeof $cssFromFile_0  === 'string' ? unsafeCSS($cssFromFile_0) : $cssFromFile_0;
registerStyles('vaadin-grid', $css_0, {moduleId: 'flow_css_mod_0'});

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '5f789ad1657a3ae7e2f8aa6899cc5475722ae96bd3b57fda5bd3be3271f18209') {
    pending.push(import('./chunks/chunk-4e2c2e8ba585871955314023b33188b731e3ef7c3aa5825b97316a6927c8bc38.js'));
  }
  if (key === 'bb5074b4da36dc950f584a3dce60ec97381c50118592657a8b9dda28e18b6d03') {
    pending.push(import('./chunks/chunk-d2b4b4e40e2fc27df43b3b599450d2935a5f5c27f8860830052e72f7319f560f.js'));
  }
  if (key === '9c9d99ec0ea027c595d28713faccd7e0a0f97373e67822a411486ce3f967c904') {
    pending.push(import('./chunks/chunk-840f021b9a1845b7d09e3f0e1ab7de99dd3cd225bb71b719736a47cb6c8adebc.js'));
  }
  if (key === 'e90f057aaed9b371a857ef302207a696006a50cc40ac7049169df55d553b6103') {
    pending.push(import('./chunks/chunk-e72f3e6a9bf305bbd5a875f992b9cd7ec35c52e238c4b3f6fe1277e019310d6b.js'));
  }
  if (key === '2481b5311276b3a58e9408750e78dcb774c741080a3aeb63b42241193c0cf95f') {
    pending.push(import('./chunks/chunk-840f021b9a1845b7d09e3f0e1ab7de99dd3cd225bb71b719736a47cb6c8adebc.js'));
  }
  if (key === '28a92dc9019b3f883c51c72df5665402e84e5ded5955d2a5892d01d5799f6d01') {
    pending.push(import('./chunks/chunk-4e2c2e8ba585871955314023b33188b731e3ef7c3aa5825b97316a6927c8bc38.js'));
  }
  if (key === '9ccf1e9d5b677c7600bbaffbc5af1472302426bd16b75877b04ae08b2e6fd323') {
    pending.push(import('./chunks/chunk-d2b4b4e40e2fc27df43b3b599450d2935a5f5c27f8860830052e72f7319f560f.js'));
  }
  if (key === '4d9bc80458c7e787ff131342fdb1bde00cec9979bf7d3db1203295d12f1450e5') {
    pending.push(import('./chunks/chunk-5b6544b4ec7e168774106b83b921977600ed2427a469635a67bce4ddeffbd940.js'));
  }
  if (key === 'edbeda5a8630934e84c6796b5e93178f40d69d348e6b653ae37e1af329835a81') {
    pending.push(import('./chunks/chunk-840f021b9a1845b7d09e3f0e1ab7de99dd3cd225bb71b719736a47cb6c8adebc.js'));
  }
  if (key === '4e657187620163e0df0e713a53931a8caa73766194f5c346a4299bdd397f3aa4') {
    pending.push(import('./chunks/chunk-4e2c2e8ba585871955314023b33188b731e3ef7c3aa5825b97316a6927c8bc38.js'));
  }
  if (key === '949f188a8a4eb5f008a4d06c986daf518c1c76b33b28b7bd60bec6897020cf55') {
    pending.push(import('./chunks/chunk-4e2c2e8ba585871955314023b33188b731e3ef7c3aa5825b97316a6927c8bc38.js'));
  }
  if (key === '06499bc549b563be71ea44e40f078e06dbda5d3a22b2cb4eed2d1cdaeb86dade') {
    pending.push(import('./chunks/chunk-a71dfc1b8b7b5b34ed9bcc92d5c474dfdcca7779f5efcabdf32ecd0a0914b856.js'));
  }
  if (key === 'e852364e6c065c4913c885ddcf88084865cb87954d1d95d49ae24df89f86aa04') {
    pending.push(import('./chunks/chunk-32a5fa05e23fb0bf7ba7ff689ea73bb771c3eca5f4421af0e95355a9db4e240f.js'));
  }
  if (key === '1ea29bd7fb43790c39c75d4e3689ef39a797d9f2214897e48906b5c659dc0934') {
    pending.push(import('./chunks/chunk-1d407095de2a4d827d55f914f0a38a722ab674f82cd50f7d0158d0f9b5278656.js'));
  }
  return Promise.all(pending);
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}