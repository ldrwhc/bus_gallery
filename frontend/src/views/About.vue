
<template>
  <div class="docs-shell">
    <aside class="docs-sidebar">
      <div class="brand">
        <p class="tag">Bus Gallery</p>
        <h1>文档</h1>
        <p>部署、接口、工作流、中间件</p>
      </div>

      <nav class="tree">
        <div class="branch">
          <button class="node l1" :class="{ active: isModule1Page }" @click="module1Expanded = !module1Expanded">
            <span>模块1 部署与运行</span><span class="chev">{{ module1Expanded ? '▾' : '▸' }}</span>
          </button>
          <div v-show="module1Expanded" class="lvl lvl2">
            <button class="node l2" :class="{ active: selectedPageId === 'm1' }" @click="selectPage('m1')">部署与运行</button>
            <button class="node l2" :class="{ active: selectedPageId === 'm1-config' }" @click="selectPage('m1-config')">配置文件建议值</button>
          </div>
        </div>

        <div class="branch">
          <button class="node l1" :class="{ active: isModule2Page }" @click="module2Expanded = !module2Expanded">
            <span>模块2 Web 接口文档</span><span class="chev">{{ module2Expanded ? '▾' : '▸' }}</span>
          </button>
          <div v-show="module2Expanded" class="lvl lvl2">
            <button class="node l2" :class="{ active: selectedPageId === 'm2-intro' }" @click="selectPage('m2-intro')">总览说明</button>
            <button class="node l2" :class="{ active: selectedPageId === 'm2-review-manage' }" @click="selectPage('m2-review-manage')">审核页车辆管理</button>

            <div class="branch">
              <button class="node l3" :class="{ active: isApiTreeActive }" @click="apiTreeExpanded = !apiTreeExpanded">
                <span>API 文档</span><span class="chev">{{ apiTreeExpanded ? '▾' : '▸' }}</span>
              </button>
              <div v-show="apiTreeExpanded" class="lvl lvl4">
                <template v-for="g in apiGroups" :key="g.key">
                  <button class="node l4 group" @click="toggleApiGroup(g.key)">
                    <span>{{ g.name }}</span><span class="count">{{ g.apis.length }}</span>
                  </button>
                  <div v-show="apiGroupExpanded[g.key]" class="lvl lvl5">
                    <button v-for="api in g.apis" :key="api.pageId" class="node l5" :class="{ active: selectedPageId === api.pageId }" @click="openApiPage(api.pageId)">
                      <span class="chip" :class="'chip--' + api.method.toLowerCase()">{{ api.method }}</span>
                      <span class="api-cn">{{ api.cnName }}</span>
                    </button>
                  </div>
                </template>
              </div>
            </div>

            <div class="branch">
              <button class="node l3" :class="{ active: isFlowTreeActive }" @click="flowTreeExpanded = !flowTreeExpanded">
                <span>全链路工作流</span><span class="chev">{{ flowTreeExpanded ? '▾' : '▸' }}</span>
              </button>
              <div v-show="flowTreeExpanded" class="lvl lvl4">
                <button v-for="f in flowPages" :key="f.pageId" class="node l4" :class="{ active: selectedPageId === f.pageId }" @click="selectPage(f.pageId)">
                  {{ f.id }} {{ f.shortTitle }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="branch">
          <button class="node l1" :class="{ active: isModule3Page }" @click="module3Expanded = !module3Expanded">
            <span>模块3 中间件视角</span><span class="chev">{{ module3Expanded ? '▾' : '▸' }}</span>
          </button>
          <div v-show="module3Expanded" class="lvl lvl2">
            <button class="node l2" :class="{ active: selectedPageId === 'm3-intro' }" @click="selectPage('m3-intro')">总览</button>
            <button v-for="m in middlewarePages" :key="m.pageId" class="node l3" :class="{ active: selectedPageId === m.pageId }" @click="selectPage(m.pageId)">{{ m.name }}</button>
          </div>
        </div>

        <button class="node l1" :class="{ active: selectedPageId === 'm4' }" @click="selectPage('m4')">模块4 安全与风控</button>
        <button class="node l1" :class="{ active: selectedPageId === 'm5' }" @click="selectPage('m5')">模块5 面试QA</button>
      </nav>
    </aside>

    <main ref="docsMainRef" class="docs-main">
      <article v-if="selectedPageId === 'm1'" class="page">
        <header><h2>模块1：项目部署与运行方法</h2></header>

        <section class="block">
          <h3>环境准备</h3>
          <div class="table-wrap"><table>
            <thead><tr><th>组件</th><th>版本建议</th><th>来源</th><th>说明</th></tr></thead>
            <tbody>
              <tr><td>Docker</td><td>24+</td><td>docker/docker-compose.yml</td><td>运行全部服务容器</td></tr>
              <tr><td>Docker Compose</td><td>v2.20+</td><td>docker/.env.example</td><td>启动/停止/日志</td></tr>
              <tr><td>JDK</td><td>17</td><td>backend/pom.xml</td><td>本地后端运行</td></tr>
              <tr><td>Node.js</td><td>18 LTS</td><td>frontend/package.json</td><td>本地前端运行</td></tr>
            </tbody>
          </table></div>
        </section>

        <section class="block">
          <h3>Docker 部署命令</h3>
          <div class="code"><pre><code>copy docker/.env.example docker/.env

docker compose --env-file docker/.env -f docker/docker-compose.yml up -d --build
docker compose --env-file docker/.env -f docker/docker-compose.yml ps
docker compose --env-file docker/.env -f docker/docker-compose.yml logs -f backend
docker compose --env-file docker/.env -f docker/docker-compose.yml stop
docker compose --env-file docker/.env -f docker/docker-compose.yml restart
docker compose --env-file docker/.env -f docker/docker-compose.yml down</code></pre></div>
        </section>

        <section class="block">
          <h3>本地开发运行</h3>
          <div class="code"><pre><code># backend
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# frontend
cd frontend
npm install
npm run dev</code></pre></div>
        </section>
      </article>
      <article v-else-if="selectedPageId === 'm1-config'" class="page">
        <header><h2>模块1：配置文件方法与建议值</h2><p>这一页给出可直接落地的配置模板、推荐值和修改步骤。</p></header>

        <section class="block">
          <h3>配置修改总表</h3>
          <div class="table-wrap"><table>
            <thead><tr><th>配置项</th><th>文件</th><th>怎么改</th><th>推荐值</th><th>影响</th></tr></thead>
            <tbody>
              <tr v-for="cfg in deployConfigs" :key="cfg.key + cfg.file">
                <td class="mono">{{ cfg.key }}</td>
                <td class="mono">{{ cfg.file }}</td>
                <td>{{ cfg.how }}</td>
                <td class="mono">{{ cfg.recommend }}</td>
                <td>{{ cfg.effect }}</td>
              </tr>
            </tbody>
          </table></div>
        </section>

        <section class="block">
          <h3>docker/.env 示例（建议值）</h3>
          <div class="code"><pre><code>{{ envConfigExample }}</code></pre></div>
        </section>

        <section class="block">
          <h3>application.yml 核心段（建议值）</h3>
          <div class="code"><pre><code>{{ appConfigExample }}</code></pre></div>
        </section>

        <section class="block">
          <h3>Nginx 限流与上传建议</h3>
          <div class="code"><pre><code>{{ nginxConfigExample }}</code></pre></div>
        </section>
      </article>
      <article v-else-if="selectedPageId === 'm2-intro'" class="page">
        <header><h2>模块2：Web 接口文档总览</h2></header>

        <section class="block">
          <h3>统一约定</h3>
          <ul>
            <li>Base URL：`/api`</li>
            <li>鉴权：`Authorization: Bearer &lt;token&gt;` 或 `?token=`</li>
            <li>错误结构：`{ code, message }`</li>
          </ul>
          <div class="code"><pre><code>{
  "code": "A0401",
  "message": "Unauthorized or session expired"
}</code></pre></div>
        </section>

        <section class="block">
          <h3>API 分类整理一览表</h3>
          <div class="table-wrap"><table>
            <thead><tr><th>分类</th><th>接口数</th><th>说明</th><th>跳转</th></tr></thead>
            <tbody>
              <tr v-for="row in apiCategoryRows" :key="row.group">
                <td>{{ row.name }}</td><td>{{ row.count }}</td><td>{{ row.intro }}</td>
                <td><button class="link" @click="openApiPage(row.firstPageId)">查看首个接口</button></td>
              </tr>
            </tbody>
          </table></div>
        </section>

        <section class="block">
          <h3>配置入口（API + 部署）</h3>
          <div class="table-wrap"><table>
            <thead><tr><th>主题</th><th>文件</th><th>建议修改项</th><th>作用</th></tr></thead>
            <tbody>
              <tr v-for="item in apiConfigEntries" :key="item.topic + item.file">
                <td>{{ item.topic }}</td><td class="mono">{{ item.file }}</td><td class="mono">{{ item.keys }}</td><td>{{ item.effect }}</td>
              </tr>
            </tbody>
          </table></div>
        </section>
      </article>
      <article v-else-if="selectedPageId === 'm2-review-manage'" class="page">
        <header>
          <h2>模块2：审核页车辆管理说明</h2>
          <p>本页对应审核中心中的“车辆管理”子页面，覆盖查、改、删能力、权限边界和接口映射。</p>
        </header>

        <section class="block">
          <h3>页面能力</h3>
          <ul>
            <li>查询：支持按车牌/自编号关键词查询；支持游标分页（lastLaunch + lastId）。</li>
            <li>查询范围会自动按审核员的省级审核区展开，省下属城市关联的车辆也会显示在管理列表中。</li>
            <li>编辑：点击“编辑”后加载车辆详情，支持修改车辆基础信息、配置信息、地区、公司、品牌、车型等。</li>
            <li>删除：点击“删除”会弹二次确认框，确认后执行删除并刷新列表。</li>
            <li>与审核页切换：在同一页面内切换“车辆审核/车辆管理”，无需路由跳转。</li>
          </ul>
        </section>

        <section class="block">
          <h3>权限规则</h3>
          <div class="table-wrap"><table>
            <thead><tr><th>角色</th><th>查询范围</th><th>编辑/删除范围</th><th>说明</th></tr></thead>
            <tbody>
              <tr><td>STATION</td><td>全站（可带 regionId 过滤）</td><td>全站</td><td>站长可跨地区维护车辆。</td></tr>
              <tr><td>REVIEWER</td><td>后端按审核地区限制，省级范围会自动覆盖本省所有市级车辆</td><td>仅审核地区（按省级归属校验）</td><td>越权请求会被拒绝并返回未授权错误。</td></tr>
            </tbody>
          </table></div>
        </section>

        <section class="block">
          <h3>接口映射</h3>
          <div class="table-wrap"><table>
            <thead><tr><th>动作</th><th>接口</th><th>说明</th><th>跳转</th></tr></thead>
            <tbody>
              <tr>
                <td>管理列表查询</td>
                <td class="mono">GET /api/vehicles/manage</td>
                <td>审核中心专用分页查询，支持 keyword、regionId、lastLaunch、lastId；当审核范围是省级时会自动包含省内市级车辆。</td>
                <td><button class="link" @click="openPage('/api/vehicles/manage')">查看 API</button></td>
              </tr>
              <tr>
                <td>详情加载</td>
                <td class="mono">GET /api/vehicles/{id}</td>
                <td>编辑弹窗打开时拉取详情数据并回填表单。</td>
                <td><button class="link" @click="openPage('/api/vehicles/{id}')">查看 API</button></td>
              </tr>
              <tr>
                <td>保存修改</td>
                <td class="mono">PUT /api/vehicles/{id}</td>
                <td>提交编辑后的车辆信息，成功后会同步刷新管理列表、本地详情缓存，并触发其他页面快照回源获取最新数据。</td>
                <td><button class="link" @click="openPage('/api/vehicles/{id}')">查看 API</button></td>
              </tr>
              <tr>
                <td>删除车辆</td>
                <td class="mono">DELETE /api/vehicles/{id}</td>
                <td>二次确认后删除车辆，成功后刷新列表并关闭可能已打开的编辑弹窗。</td>
                <td><button class="link" @click="openPage('/api/vehicles/{id}')">查看 API</button></td>
              </tr>
            </tbody>
          </table></div>
        </section>

        <section class="block">
          <h3>常见问题与处理</h3>
          <ul>
            <li>401 未授权：通常是会话失效或角色不满足 `REVIEWER/STATION`，先重新登录并确认角色。</li>
            <li>车辆管理列表为空：若审核范围是省级，后端现在会自动展开到省内所有市级车辆；若仍为空，优先检查审核地区分配是否正确。</li>
            <li>审核员跨区修改失败：后端会按审核地区做写权限校验，非本审核范围车辆不可改删。</li>
            <li>修改后其他页面仍是旧数据：当前写链路会同步本地缓存并清理车辆分页/车牌快照缓存，若线上仍旧数据，优先排查 Redis 是否可写。</li>
            <li>删除失败（外键约束）：需先清理关联的收藏/评论等子表记录，再删除主车辆记录。</li>
          </ul>
        </section>
      </article>

      <article v-else-if="currentApi" class="page api-doc">
        <header class="api-hero">
          <div class="api-hero__title">
            <span class="chip" :class="'chip--' + currentApi.method.toLowerCase()">{{ currentApi.method }}</span>
            <h2>{{ currentApi.cnName }}</h2>
          </div>
          <p class="path">{{ currentApi.path }}</p>
          <p>{{ currentApi.desc }}</p>
          <div class="api-meta">
            <span class="meta-pill">所属模块：{{ currentApi.groupName }}</span>
            <span class="meta-pill">请求头：{{ currentApi.headers }}</span>
            <button class="meta-pill link-btn" @click="openFlowPage(currentApi.flow)">关联工作流：{{ currentApi.flow }} {{ flowTitleMap[currentApi.flow] }}</button>
          </div>
        </header>

        <div class="api-layout">
          <div class="api-main">
            <section class="block api-card">
              <h3>接口定义</h3>
              <div class="endpoint-line">
                <span class="chip" :class="'chip--' + currentApi.method.toLowerCase()">{{ currentApi.method }}</span>
                <code>{{ currentApi.path }}</code>
              </div>
              <p>{{ currentApi.desc }}</p>
            </section>

            <section class="block api-card" v-for="sec in apiParamSections(currentApi)" :key="currentApi.pageId + sec.key">
              <h3>{{ sec.title }}</h3>
              <div class="table-wrap"><table class="param-table">
                <thead><tr><th>参数名</th><th>类型</th><th>必填</th><th>说明</th><th>示例</th></tr></thead>
                <tbody>
                  <tr v-for="row in sec.rows" :key="sec.key + row.name">
                    <td class="mono">{{ row.name }}</td>
                    <td class="mono">{{ row.type }}</td>
                    <td>{{ row.required ? '是' : '否' }}</td>
                    <td>{{ row.desc }}</td>
                    <td class="mono">{{ row.example }}</td>
                  </tr>
                </tbody>
              </table></div>
            </section>

            <section class="block api-card">
              <h3>状态码</h3>
              <div class="table-wrap"><table class="param-table">
                <thead><tr><th>状态码</th><th>含义</th><th>触发条件</th></tr></thead>
                <tbody>
                  <tr v-for="status in apiStatusRows(currentApi)" :key="currentApi.pageId + status.code">
                    <td class="mono">{{ status.code }}</td>
                    <td>{{ status.meaning }}</td>
                    <td>{{ status.when }}</td>
                  </tr>
                </tbody>
              </table></div>
            </section>

            <section class="block api-card">
              <h3>请求参数（JSON 注释）</h3>
              <div class="code"><pre><code>{{ currentApi.paramJson }}</code></pre></div>
            </section>

            <section class="block api-card">
              <h3>配置修改建议</h3>
              <div class="table-wrap"><table>
                <thead><tr><th>配置项</th><th>文件</th><th>如何修改</th><th>影响</th></tr></thead>
                <tbody>
                  <tr v-for="cfg in currentApi.configHints" :key="cfg.key + cfg.file">
                    <td class="mono">{{ cfg.key }}</td><td class="mono">{{ cfg.file }}</td><td>{{ cfg.how }}</td><td>{{ cfg.effect }}</td>
                  </tr>
                </tbody>
              </table></div>
            </section>
          </div>

          <aside class="api-aside">
            <section class="block api-card api-sticky">
              <h3>请求示例</h3>
              <div class="code"><pre><code>{{ currentApi.reqExample }}</code></pre></div>
              <h3>成功响应</h3>
              <div class="code"><pre><code>{{ currentApi.successExample }}</code></pre></div>
              <h3>失败响应</h3>
              <div class="code"><pre><code>{{ currentApi.errorExample }}</code></pre></div>
            </section>
          </aside>
        </div>
      </article>

      <article v-else-if="currentFlow" class="page">
        <header><h2>{{ currentFlow.id }} {{ currentFlow.title }}</h2><p>{{ currentFlow.intro }}</p></header>

        <section class="block">
          <h3>数据流向时序图（专业视图）</h3>
          <div class="stripe-seq" @mousemove="handleSeqMousemove" @mouseleave="clearActiveLane">
            <div class="lane-hover-tip" v-if="laneHover.visible" :style="laneHoverStyle">{{ laneHover.text }}</div>
            <div class="stripe-head" :class="{ active: activeLane >= 0 }">
              <div
                v-for="(lane, idx) in currentFlow.swimlanes"
                :key="currentFlow.id + lane"
                class="stripe-lane"
                :class="{ active: activeLane === idx }"
                @mouseenter="setActiveLane(idx)"
                @mouseleave="clearActiveLane"
              >
                <span class="lane-index">{{ idx + 1 }}</span>
                <span>{{ lane }}</span>
              </div>
            </div>
            <div class="stripe-body">
              <div class="flow-track">
                <span class="track-title">主流程</span>
                <div class="track-chain">
                  <template v-for="(node, idx) in currentFlow.mainPath" :key="currentFlow.id + node.name + idx">
                    <button class="track-node" :class="{ active: activeLane === node.lane }" @mouseenter="setActiveLane(node.lane)" @mouseleave="clearActiveLane" @click="openPage(node.targetPage)">
                      {{ node.name }}
                    </button>
                    <span v-if="idx < currentFlow.mainPath.length - 1" class="track-arrow">╌╌▶</span>
                  </template>
                </div>
              </div>
              <div class="lane-guides">
                <div v-for="(lane, idx) in currentFlow.swimlanes" :key="currentFlow.id + lane + '-guide'" class="lane-guide" :class="{ active: activeLane === idx }"></div>
              </div>
              <div v-for="(evt, idx) in currentFlow.events" :key="currentFlow.id + evt.title + idx" class="stripe-event">
                <div class="event-order">{{ idx + 1 }}</div>
                <div class="event-stack" :style="eventSpanStyle(evt)">
                  <div
                    class="event-line"
                    :class="{ reverse: evt.from > evt.to, self: evt.from === evt.to, active: eventTouchesLane(evt, activeLane) }"
                  ></div>
                  <div class="event-connector" :class="[eventConnectorClass(evt), { active: eventTouchesLane(evt, activeLane) }]"></div>
                  <div class="event-card" :class="{ active: eventTouchesLane(evt, activeLane) }">
                    <div class="row">
                      <h4>{{ evt.title }}</h4>
                      <button class="link" @click="openPage(evt.targetPage)">{{ evt.targetText }}</button>
                    </div>
                    <p>{{ evt.text }}</p>
                    <div class="seq-badges">
                      <span v-for="tag in evt.tags" :key="evt.title + tag" class="badge">{{ tag }}</span>
                    </div>
                    <div class="refs">
                      <button v-for="api in evt.apis" :key="evt.title + api.pageId" class="ref" @click="openApiPage(api.pageId)">
                        <span class="chip" :class="'chip--' + api.method.toLowerCase()">{{ api.method }}</span>{{ api.cnName }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="block">
          <h3>分层流程图（保留）</h3>
          <div class="layer-flow">
            <template v-for="(layer, idx) in currentFlow.layerFlow" :key="layer.name">
              <div class="layer-card">
                <h4>{{ layer.name }}</h4>
                <p>{{ layer.text }}</p>
              </div>
              <div v-if="idx < currentFlow.layerFlow.length - 1" class="layer-arrow">→</div>
            </template>
          </div>
        </section>

        <section class="block">
          <h3>面试高频标注说明</h3>
          <ul class="notes">
            <li v-for="note in currentFlow.notes" :key="currentFlow.id + note.tag">
              <strong>{{ note.tag }}</strong> {{ note.title }}：{{ note.explain }}
            </li>
          </ul>
        </section>
      </article>
      <article v-else-if="selectedPageId === 'm3-intro'" class="page">
        <header><h2>模块3：中间件视角总览</h2></header>
        <section class="block">
          <h3>触发条件并列事件（带超链接）</h3>
          <div class="table-wrap"><table>
            <thead><tr><th>触发条件</th><th>执行组件</th><th>执行动作</th><th>跳转</th></tr></thead>
            <tbody>
              <tr v-for="e in middlewareEvents" :key="e.trigger + e.flow">
                <td>{{ e.trigger }}</td><td>{{ e.exec }}</td><td>{{ e.action }}</td>
                <td><div class="jump">
                  <button class="link" @click="openFlowPage(e.flow)">{{ e.flow }}</button>
                  <button v-for="mw in e.middlewarePages" :key="e.flow + mw" class="link" @click="selectPage(mw)">{{ middlewareMap[mw].name }}</button>
                </div></td>
              </tr>
            </tbody>
          </table></div>
        </section>
      </article>

      <article v-else-if="currentMiddleware" class="page">
        <header><h2>{{ currentMiddleware.name }}</h2><p>{{ currentMiddleware.intro }}</p></header>
        <section class="block"><h3>核心作用</h3><p>{{ currentMiddleware.role }}</p></section>

        <section class="block">
          <h3>输入 / 处理 / 输出</h3>
          <div class="table-wrap"><table><thead><tr><th>输入</th><th>处理</th><th>输出</th></tr></thead><tbody>
            <tr v-for="io in currentMiddleware.io" :key="io.input + io.output"><td>{{ io.input }}</td><td>{{ io.process }}</td><td>{{ io.output }}</td></tr>
          </tbody></table></div>
        </section>

        <section class="block">
          <h3>配置项与功能映射</h3>
          <div class="table-wrap"><table><thead><tr><th>配置项</th><th>文件</th><th>作用</th></tr></thead><tbody>
            <tr v-for="cfg in currentMiddleware.configs" :key="cfg.key + cfg.file"><td class="mono">{{ cfg.key }}</td><td class="mono">{{ cfg.file }}</td><td>{{ cfg.effect }}</td></tr>
          </tbody></table></div>
        </section>

        <section class="block">
          <h3>参与工作流（输入/输出）</h3>
          <div class="table-wrap"><table><thead><tr><th>工作流</th><th>输入</th><th>处理中间件动作</th><th>输出</th></tr></thead><tbody>
            <tr v-for="wf in currentMiddleware.workflows" :key="wf.flow + wf.input">
              <td><button class="link" @click="openFlowPage(wf.flow)">{{ wf.flow }}</button></td><td>{{ wf.input }}</td><td>{{ wf.process }}</td><td>{{ wf.output }}</td>
            </tr>
          </tbody></table></div>
        </section>

        <section v-if="currentMiddleware.name === 'Redis'" class="block redis-hero-block">
          <div class="redis-hero">
            <div class="redis-hero-main">
              <span class="redis-kicker">BUS GALLERY CACHE CONTROL</span>
              <h3>Redis 一眼看懂</h3>
              <p>把这页当成缓存驾驶舱来看：左边看职责分层，右边看规模；再往下看写后读一致性和键空间地图，就能快速定位“为什么这里会读到旧数据”。</p>
              <div class="redis-hero-tags">
                <span v-for="item in redisOperationsOverview" :key="item.title" class="redis-hero-tag" :class="'tone-' + item.tone">{{ item.title }}</span>
              </div>
            </div>
            <div class="redis-hero-stats">
              <article v-for="item in redisHeroStats" :key="item.label" class="redis-stat-card">
                <div class="redis-stat-value">{{ item.value }}</div>
                <div class="redis-stat-label">{{ item.label }}</div>
                <p>{{ item.note }}</p>
              </article>
            </div>
          </div>
        </section>

        <section v-if="currentMiddleware.name === 'Redis'" class="block">
          <h3>职责分层</h3>
          <div class="redis-overview-grid">
            <article v-for="item in redisOperationsOverview" :key="item.title" class="redis-overview-card" :class="'tone-' + item.tone">
              <div class="redis-overview-head">
                <h4>{{ item.title }}</h4>
                <span class="redis-overview-badge">{{ item.badge }}</span>
              </div>
              <p>{{ item.desc }}</p>
              <div class="redis-overview-meta">
                <span><strong>代表键：</strong>{{ item.key }}</span>
                <span><strong>动作：</strong>{{ item.action }}</span>
              </div>
            </article>
          </div>
        </section>

        <section v-if="currentMiddleware.name === 'Redis'" class="block">
          <h3>写后读一致性策略</h3>
          <div class="redis-strategy-grid">
            <article v-for="item in redisConsistencyCards" :key="item.title" class="redis-strategy-card" :class="'tone-' + item.tone">
              <div class="redis-strategy-title">{{ item.title }}</div>
              <p>{{ item.desc }}</p>
              <div class="redis-strategy-kv"><span>触发时机</span><strong>{{ item.trigger }}</strong></div>
              <div class="redis-strategy-kv"><span>关键键</span><code>{{ item.key }}</code></div>
              <div class="redis-strategy-kv"><span>结果</span><strong>{{ item.result }}</strong></div>
            </article>
          </div>
        </section>

        <section v-if="currentMiddleware.name === 'Redis'" class="block">
          <h3>Redis 键空间地图</h3>
          <div class="redis-key-groups">
            <article v-for="group in redisKeyGroupCards" :key="group.name" class="redis-key-group" :class="'tone-' + group.tone">
              <div class="redis-key-group-head">
                <div>
                  <h4>{{ group.name }}</h4>
                  <p>{{ group.summary }}</p>
                </div>
                <span class="redis-key-group-count">{{ group.count }} 类键</span>
              </div>
              <div class="redis-key-group-list">
                <div v-for="row in group.rows" :key="group.name + row.key" class="redis-key-card">
                  <div class="redis-key-card-head">
                    <code>{{ row.key }}</code>
                    <span class="redis-key-card-ttl">{{ row.ttl }}</span>
                  </div>
                  <p>{{ row.usage }}</p>
                  <div class="redis-key-card-meta">
                    <span>创建：{{ row.created }}</span>
                    <span>更新：{{ row.updated }}</span>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section v-if="currentMiddleware.name === 'Redis'" class="block">
          <div class="redis-archive-head">
            <div>
              <h3>Redis 全量键值清单（细表备查）</h3>
              <p>这部分默认收起，只在需要核对具体 key 模板、TTL、删除时机时再展开，避免阅读节奏被超长表格打断。</p>
            </div>
            <button class="redis-archive-toggle" @click="redisArchiveOpen = !redisArchiveOpen">
              {{ redisArchiveOpen ? '收起明细表' : '展开明细表' }}
            </button>
          </div>
          <div class="redis-archive-toolbar">
            <div class="redis-archive-stats">
              <span class="redis-archive-pill">当前显示 {{ filteredRedisKeyRows.length }} / {{ redisKeyRows.length }}</span>
              <span class="redis-archive-pill">{{ redisArchiveGroup === '全部' ? '全部分类' : redisArchiveGroup }}</span>
            </div>
            <label class="redis-archive-search">
              <span>搜索</span>
              <input v-model="redisArchiveKeyword" type="text" placeholder="按 key / TTL / 用途 / 时机搜索" />
            </label>
          </div>
          <div class="redis-archive-groups">
            <button
              v-for="group in redisArchiveGroups"
              :key="group"
              class="redis-archive-chip"
              :class="{ active: redisArchiveGroup === group }"
              @click="redisArchiveGroup = group"
            >
              {{ group }}
            </button>
          </div>
          <div v-if="redisArchiveOpen" class="table-wrap redis-archive-table">
            <table>
              <thead><tr><th>分类</th><th>Key</th><th>Value</th><th>TTL/失效</th><th>产生时机</th><th>更新/删除时机</th><th>用途</th></tr></thead>
              <tbody>
                <tr v-for="row in filteredRedisKeyRows" :key="row.key">
                  <td>{{ row.group }}</td>
                  <td class="mono">{{ row.key }}</td>
                  <td class="mono">{{ row.value }}</td>
                  <td class="mono">{{ row.ttl }}</td>
                  <td>{{ row.created }}</td>
                  <td>{{ row.updated }}</td>
                  <td>{{ row.usage }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="redis-archive-preview">
            <article v-for="row in redisArchivePreviewRows" :key="'preview-' + row.key" class="redis-archive-preview-card">
              <div class="redis-archive-preview-head">
                <span class="redis-archive-preview-group">{{ row.group }}</span>
                <span class="redis-archive-preview-ttl">{{ row.ttl }}</span>
              </div>
              <code>{{ row.key }}</code>
              <p>{{ row.usage }}</p>
            </article>
          </div>
        </section>

        <section v-if="currentMiddleware.name === 'Redis'" class="block">
          <h3>Redis 面试重点（按业务流程展开）</h3>
          <div v-for="item in redisInterviewFlowViews" :key="item.id" class="redis-flow">
            <h4 class="redis-flow-title">
              <span class="redis-flow-title-text">{{ item.id }} {{ item.title }}</span>
              <button class="flow-open-btn" @click="openRedisFlowDialog(item)">弹出流程图</button>
            </h4>
            <p><strong>涉及键：</strong><span class="mono">{{ item.keys }}</span></p>
            <ul>
              <li v-for="step in item.steps" :key="item.id + step">{{ step }}</li>
            </ul>
            <div class="redis-sequence" v-if="item.diagram && item.diagram.entries.length">
              <div class="redis-sequence-legend">
                <span class="redis-legend-item"><i class="main"></i>主调用</span>
                <span class="redis-legend-item"><i class="reply"></i>返回/回包</span>
                <span class="redis-legend-item"><i class="alt"></i>如果分支</span>
                <span class="redis-legend-item"><i class="else"></i>否则分支</span>
              </div>
              <div class="redis-sequence-scroll">
                <svg
                  class="redis-sequence-canvas"
                  :width="item.diagram.width"
                  :height="item.diagram.height"
                  :viewBox="`0 0 ${item.diagram.width} ${item.diagram.height}`"
                  preserveAspectRatio="xMidYMin meet"
                >
                  <rect
                    class="redis-sequence-surface"
                    x="1"
                    y="1"
                    :width="item.diagram.width - 2"
                    :height="item.diagram.height - 2"
                    rx="28"
                  />

                  <g v-for="lane in item.diagram.lanes" :key="item.id + '-lane-' + lane.label">
                    <line
                      class="redis-sequence-lane-line"
                      :x1="lane.x"
                      :x2="lane.x"
                      :y1="lane.lineY1"
                      :y2="lane.lineY2"
                    />
                    <rect
                      class="redis-sequence-lane-card"
                      :x="lane.cardX"
                      :y="lane.cardY"
                      :width="lane.cardWidth"
                      :height="lane.cardHeight"
                      rx="18"
                      :fill="lane.bg"
                      :stroke="lane.border"
                    />
                    <rect
                      class="redis-sequence-lane-badge"
                      :x="lane.badgeX"
                      :y="lane.badgeY"
                      :width="lane.badgeWidth"
                      :height="lane.badgeHeight"
                      rx="11"
                    />
                    <text
                      class="redis-sequence-lane-badge-text mono"
                      :x="lane.x"
                      :y="lane.badgeTextY"
                      text-anchor="middle"
                    >
                      {{ lane.index + 1 }}
                    </text>
                    <text
                      class="redis-sequence-lane-label"
                      :x="lane.x"
                      :y="lane.labelY"
                      :fill="lane.ink"
                      text-anchor="middle"
                    >
                      <tspan
                        v-for="(line, idx) in lane.labelLines"
                        :key="item.id + '-lane-text-' + lane.index + '-' + idx"
                        :x="lane.x"
                        :dy="idx === 0 ? 0 : 20"
                      >
                        {{ line }}
                      </tspan>
                    </text>
                  </g>

                  <g v-for="entry in item.diagram.entries" :key="item.id + '-entry-' + entry.step + '-' + entry.type">
                    <title>{{ entry.detail }}</title>

                    <template v-if="entry.type === 'message'">
                      <line
                        v-if="entry.connectorX"
                        class="redis-sequence-card-link"
                        :x1="entry.connectorX"
                        :x2="entry.connectorX"
                        :y1="entry.cardY + entry.cardHeight"
                        :y2="entry.lineY - 8"
                        :stroke="entry.border"
                      />
                      <line
                        v-if="!entry.isSelf"
                        class="redis-sequence-message-line"
                        :x1="entry.fromX"
                        :y1="entry.lineY"
                        :x2="entry.toX"
                        :y2="entry.lineY"
                        :stroke="entry.stroke"
                        :stroke-dasharray="entry.isReply ? '8 6' : undefined"
                      />
                      <path
                        v-else
                        class="redis-sequence-message-line"
                        :d="entry.selfPath"
                        :stroke="entry.stroke"
                        :stroke-dasharray="entry.isReply ? '8 6' : undefined"
                      />
                      <circle :cx="entry.fromX" :cy="entry.lineY" r="4" :fill="entry.stroke" opacity="0.32" />
                      <circle v-if="!entry.isSelf" :cx="entry.toX" :cy="entry.lineY" r="4.4" :fill="entry.stroke" />
                      <polygon :points="entry.arrowHead" :fill="entry.stroke" />

                      <rect
                        class="redis-sequence-card"
                        :x="entry.cardX"
                        :y="entry.cardY"
                        :width="entry.cardWidth"
                        :height="entry.cardHeight"
                        rx="18"
                        :fill="entry.fill"
                        :stroke="entry.border"
                      />
                      <rect
                        class="redis-sequence-step-pill"
                        :x="entry.stepBoxX"
                        :y="entry.stepBoxY"
                        :width="entry.stepBoxWidth"
                        :height="entry.stepBoxHeight"
                        rx="11"
                        :fill="entry.chipFill"
                      />
                      <text
                        class="redis-sequence-step-text mono"
                        :x="entry.stepTextX"
                        :y="entry.stepTextY"
                        :fill="entry.chipInk"
                        text-anchor="middle"
                      >
                        {{ entry.step }}
                      </text>
                      <text
                        class="redis-sequence-route-text"
                        :x="entry.routeX"
                        :y="entry.routeY"
                        :fill="entry.ink"
                      >
                        <tspan
                          v-for="(line, idx) in entry.routeLines"
                          :key="item.id + '-route-' + entry.step + '-' + idx"
                          :x="entry.routeX"
                          :dy="idx === 0 ? 0 : 20"
                        >
                          {{ line }}
                        </tspan>
                      </text>
                      <text
                        class="redis-sequence-action-text"
                        :x="entry.textX"
                        :y="entry.actionY"
                        :fill="entry.textFill"
                      >
                        <tspan
                          v-for="(line, idx) in entry.actionLines"
                          :key="item.id + '-action-' + entry.step + '-' + idx"
                          :x="entry.textX"
                          :dy="idx === 0 ? 0 : 20"
                        >
                          {{ line }}
                        </tspan>
                      </text>
                      <text
                        class="redis-sequence-hint-text"
                        :x="entry.textX"
                        :y="entry.hintY"
                        :fill="entry.hintFill"
                      >
                        <tspan
                          v-for="(line, idx) in entry.hintLines"
                          :key="item.id + '-hint-' + entry.step + '-' + idx"
                          :x="entry.textX"
                          :dy="idx === 0 ? 0 : 20"
                        >
                          {{ line }}
                        </tspan>
                      </text>
                      <g v-if="entry.branchTagText">
                        <rect
                          class="redis-sequence-tag-pill"
                          :x="entry.tagX"
                          :y="entry.tagY"
                          :width="entry.tagWidth"
                          :height="entry.tagHeight"
                          rx="10"
                          :fill="entry.chipFill"
                        />
                        <text
                          class="redis-sequence-tag-text"
                          :x="entry.tagTextX"
                          :y="entry.tagTextY"
                          :fill="entry.chipInk"
                        >
                          {{ entry.branchTagText }}
                        </text>
                      </g>
                    </template>

                    <template v-else>
                      <rect
                        class="redis-sequence-branch-box"
                        :x="entry.boxX"
                        :y="entry.boxY"
                        :width="entry.boxWidth"
                        :height="entry.boxHeight"
                        rx="18"
                        :fill="entry.fill"
                        :stroke="entry.border"
                        stroke-dasharray="8 6"
                      />
                      <rect
                        class="redis-sequence-step-pill"
                        :x="entry.stepBoxX"
                        :y="entry.stepBoxY"
                        :width="entry.stepBoxWidth"
                        :height="entry.stepBoxHeight"
                        rx="11"
                        :fill="entry.chipFill"
                      />
                      <text
                        class="redis-sequence-step-text mono"
                        :x="entry.stepTextX"
                        :y="entry.stepTextY"
                        :fill="entry.chipInk"
                        text-anchor="middle"
                      >
                        {{ entry.step }}
                      </text>
                      <text
                        class="redis-sequence-branch-title-text"
                        :x="entry.titleX"
                        :y="entry.titleY"
                        :fill="entry.textFill"
                      >
                        <tspan
                          v-for="(line, idx) in entry.titleLines"
                          :key="item.id + '-branch-title-' + entry.step + '-' + idx"
                          :x="entry.titleX"
                          :dy="idx === 0 ? 0 : 20"
                        >
                          {{ line }}
                        </tspan>
                      </text>
                      <text
                        class="redis-sequence-branch-tip-text"
                        :x="entry.tipX"
                        :y="entry.tipY"
                        :fill="entry.hintFill"
                      >
                        <tspan
                          v-for="(line, idx) in entry.tipLines"
                          :key="item.id + '-branch-tip-' + entry.step + '-' + idx"
                          :x="entry.tipX"
                          :dy="idx === 0 ? 0 : 20"
                        >
                          {{ line }}
                        </tspan>
                      </text>
                    </template>
                  </g>
                </svg>
              </div>
            </div>
          </div>
        </section>

        <div v-if="redisFlowDialog.visible" class="redis-flow-modal-mask" @click.self="closeRedisFlowDialog">
          <div class="redis-flow-modal">
            <div class="redis-flow-modal-head">
              <h4>{{ redisFlowDialog.title }}（流程图）</h4>
              <button class="flow-close-btn" @click="closeRedisFlowDialog">关闭</button>
            </div>
            <div class="redis-flow-modal-body">
              <div class="redis-flow-modal-lanes">
                <span v-for="lane in redisFlowDialog.lanes" :key="'popup-' + lane" class="redis-flow-lane-chip">{{ lane }}</span>
              </div>
              <div class="redis-flowchart">
                <div class="redis-fc-node start">开始</div>
                <div class="redis-fc-arrow">↓</div>

                <template v-for="(step, idx) in redisFlowDialog.chart.pre" :key="'fc-pre-' + idx + step.title">
                  <div class="redis-fc-node process">
                    <div class="redis-fc-title">{{ step.title }}</div>
                    <div class="redis-fc-detail">{{ step.detail }}</div>
                  </div>
                  <div class="redis-fc-arrow">↓</div>
                </template>

                <template v-if="redisFlowDialog.chart.hasBranch">
                  <div class="redis-fc-decision-wrap">
                    <div class="redis-fc-node decision">
                      <span>{{ redisFlowDialog.chart.decisionText }}</span>
                    </div>
                  </div>
                  <div class="redis-fc-decision-note">
                    <span>判断条件：{{ redisFlowDialog.chart.decisionText }}</span>
                  </div>

                  <div class="redis-fc-branches">
                    <div class="redis-fc-branch yes">
                      <div class="redis-fc-branch-head">{{ redisFlowDialog.chart.yesLabel }}</div>
                      <template v-for="(step, idx) in redisFlowDialog.chart.yes" :key="'fc-yes-' + idx + step.title">
                        <div class="redis-fc-node process branch">
                          <div class="redis-fc-title">{{ step.title }}</div>
                          <div class="redis-fc-detail">{{ step.detail }}</div>
                        </div>
                        <div v-if="idx < redisFlowDialog.chart.yes.length - 1" class="redis-fc-arrow">↓</div>
                      </template>
                    </div>
                    <div class="redis-fc-branch no">
                      <div class="redis-fc-branch-head">{{ redisFlowDialog.chart.noLabel }}</div>
                      <template v-for="(step, idx) in redisFlowDialog.chart.no" :key="'fc-no-' + idx + step.title">
                        <div class="redis-fc-node process branch">
                          <div class="redis-fc-title">{{ step.title }}</div>
                          <div class="redis-fc-detail">{{ step.detail }}</div>
                        </div>
                        <div v-if="idx < redisFlowDialog.chart.no.length - 1" class="redis-fc-arrow">↓</div>
                      </template>
                    </div>
                  </div>

                  <div class="redis-fc-merge-wrap">
                    <div class="redis-fc-node merge">合流</div>
                  </div>
                  <div class="redis-fc-arrow">↓</div>
                </template>

                <template v-for="(step, idx) in redisFlowDialog.chart.post" :key="'fc-post-' + idx + step.title">
                  <div class="redis-fc-node process">
                    <div class="redis-fc-title">{{ step.title }}</div>
                    <div class="redis-fc-detail">{{ step.detail }}</div>
                  </div>
                  <div class="redis-fc-arrow">↓</div>
                </template>

                <div class="redis-fc-node end">结束</div>
              </div>
            </div>
          </div>
        </div>
      </article>

      <article v-else-if="selectedPageId === 'm4'" class="page">
        <header><h2>模块4：安全与风控相关设计</h2></header>
        <section class="block"><h3>认证与授权</h3><ul>
          <li>Redis Session Token（非 JWT），由 `AuthTokenInterceptor` 解析。</li>
          <li>`@RequireLogin` + `RoleGuard` 控制接口访问与角色权限。</li>
          <li>密码使用 `BCryptPasswordEncoder` 哈希存储。</li>
        </ul></section>
        <section class="block"><h3>接口安全</h3><ul>
          <li>Nginx `limit_req` + `RateLimitService` 双层限流。</li>
          <li>`HumanVerificationService` 提供验证码与风险计数。</li>
          <li>`UploadSecurityService` 校验 MIME、魔数、尺寸、像素和频率。</li>
          <li>`IdempotencyService` 用 Redis setIfAbsent 防重复提交。</li>
        </ul></section>
        <section class="block"><h3>数据安全与风控</h3><ul>
          <li>`ImageAccessService`：HMAC-SHA256 + 过期时间签名访问。</li>
          <li>异常登录/异常上传触发拦截并返回统一错误码。</li>
          <li>`/api/metrics/db` 输出慢 SQL 采样用于审计与监控。</li>
        </ul></section>
      </article>

      <article v-else-if="selectedPageId === 'm5'" class="page">
        <header>
          <h2>模块5：面试 QA（30 问）</h2>
          <p>以下问答基于当前项目可运行状态，从“可上线、可扩展、可治理”的视角回答高频深挖问题。</p>
        </header>
        <section class="block">
          <h3>问题与回答</h3>
          <div class="table-wrap"><table>
            <thead><tr><th style="width:48px">#</th><th>面试问题</th><th>回答（基于当前项目）</th></tr></thead>
            <tbody>
              <tr v-for="item in module5InterviewQa" :key="'m5-' + item.id">
                <td class="mono">{{ item.id }}</td>
                <td>{{ item.q }}</td>
                <td>{{ item.a }}</td>
              </tr>
            </tbody>
          </table></div>
        </section>
      </article>

      <article v-else class="page">
        <header><h2>文档页不存在</h2></header>
        <section class="block"><p>请从左侧目录重新选择页面。</p></section>
      </article>
    </main>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue';

const rawApiLines = `
GET|/api/admin/overview
GET|/api/admin/submissions
GET|/api/admin/images/suspects
POST|/api/admin/images/suspects/cleanup
GET|/api/admin/tables/brands
POST|/api/admin/tables/brands
DELETE|/api/admin/tables/brands/{id}
PUT|/api/admin/tables/brands/{id}
GET|/api/admin/tables/companies
POST|/api/admin/tables/companies
DELETE|/api/admin/tables/companies/{id}
PUT|/api/admin/tables/companies/{id}
GET|/api/admin/tables/models
POST|/api/admin/tables/models
DELETE|/api/admin/tables/models/{id}
PUT|/api/admin/tables/models/{id}
GET|/api/admin/tables/regions
POST|/api/admin/tables/regions
DELETE|/api/admin/tables/regions/{id}
PUT|/api/admin/tables/regions/{id}
GET|/api/admin/users
PUT|/api/admin/users/{id}/role
GET|/api/auth/captcha
POST|/api/auth/email/bind/confirm
POST|/api/auth/email/bind/send-email-code
POST|/api/auth/login
POST|/api/auth/logout
POST|/api/auth/password/change/confirm
POST|/api/auth/password/change/send-email-code
POST|/api/auth/password/forgot/reset
POST|/api/auth/password/forgot/send-email-code
POST|/api/auth/password/forgot/verify-email-code
POST|/api/auth/register
POST|/api/auth/register/send-email-code
GET|/api/brands
POST|/api/brands
DELETE|/api/brands/{id}
GET|/api/brands/{id}
PUT|/api/brands/{id}
GET|/api/brands/{id}/models
GET|/api/brands/{id}/model-summaries
GET|/api/catalog/brands
GET|/api/catalog/companies
GET|/api/catalog/models
GET|/api/catalog/regions
GET|/api/companies
POST|/api/companies
DELETE|/api/companies/{id}
GET|/api/companies/{id}
PUT|/api/companies/{id}
GET|/api/companies/{id}/model-summaries
GET|/api/companies/{id}/vehicles
GET|/api/favorites
GET|/api/favorites/{vehicleId}/summary
POST|/api/favorites/{vehicleId}/toggle
DELETE|/api/images/{id}
GET|/api/images/{id}
PUT|/api/images/{id}
GET|/api/images/access/{token}
GET|/api/images/latest
POST|/api/images/upload
GET|/api/images/vehicle/{vehicleId}
GET|/api/metrics/db
GET|/api/models
POST|/api/models
DELETE|/api/models/{id}
GET|/api/models/{id}
PUT|/api/models/{id}
GET|/api/models/{id}/company-summaries
GET|/api/models/{id}/vehicles
GET|/api/regions
POST|/api/regions
DELETE|/api/regions/{id}
GET|/api/regions/{id}
PUT|/api/regions/{id}
GET|/api/regions/{id}/companies
POST|/api/reviews/{id}/approve
POST|/api/reviews/{id}/reject
GET|/api/reviews/inbox
GET|/api/reviews/pending
POST|/api/reviews/submissions/update
GET|/api/snapshots/hot
GET|/api/snapshots/plate/{plateNumber}
POST|/api/upload
GET|/api/users/{userId}
GET|/api/users/{userId}/images
GET|/api/users/me
PUT|/api/users/me/display-name
GET|/api/users/me/images
GET|/api/vehicles
GET|/api/vehicles/manage
POST|/api/vehicles
DELETE|/api/vehicles/{id}
GET|/api/vehicles/{id}
PUT|/api/vehicles/{id}
GET|/api/vehicles/{vehicleId}/comments
POST|/api/vehicles/{vehicleId}/comments
GET|/api/vehicles/plate/{plateNumber}
`;
const groupMeta = {
  Auth: { name: '认证与账号', intro: '登录注册、验证码、找回密码' },
  User: { name: '用户中心', intro: '用户资料与图片' },
  Vehicle: { name: '车辆数据', intro: '车辆列表、详情、维护' },
  Comment: { name: '评论互动', intro: '评论查询与发布' },
  Favorite: { name: '收藏互动', intro: '收藏切换与摘要' },
  Upload: { name: '上传提交流程', intro: '上传图片与车辆信息' },
  Image: { name: '图片资源', intro: '图片读取、上传与签名访问' },
  Review: { name: '审核中心', intro: '审核收件箱与审批' },
  Admin: { name: '后台管理', intro: '站长管理与字典维护' },
  Catalog: { name: '目录聚合', intro: '筛选项聚合查询' },
  Region: { name: '地区主数据', intro: '地区查询与维护' },
  Company: { name: '公司主数据', intro: '公司查询与维护' },
  Brand: { name: '品牌主数据', intro: '品牌查询与维护' },
  Model: { name: '车型主数据', intro: '车型查询与维护' },
  Snapshot: { name: '快照聚合', intro: '车牌快照与热门快照' },
  Metrics: { name: '监控指标', intro: '数据库指标' }
};
const groupOrder = ['Auth', 'User', 'Vehicle', 'Comment', 'Favorite', 'Upload', 'Image', 'Review', 'Admin', 'Catalog', 'Region', 'Company', 'Brand', 'Model', 'Snapshot', 'Metrics'];

const flowRows = [
  {
    id: 'WF-01',
    title: '车辆主链路',
    frontend: 'Gallery 与 VehicleDetail 页面在初始化和筛选变更时把 regionId、companyId、brandId、modelId、keyword 与游标参数组装成请求并发到 /api/vehicles、/api/vehicles/{id}、/api/vehicles/plate/{plateNumber}，然后把返回数据渲染到列表和详情组件。',
    nginx: 'Nginx 从浏览器接收 /api 请求后先应用 api 级限流并补齐 X-Forwarded-* 头，再把请求转发到 backend:8080 对应控制器。',
    redis: 'Spring 读取车辆分页和详情前会先查 Redis 的车辆缓存键，命中则直接回包，未命中则等待 MySQL 结果回填缓存，并在写链路后通过版本键触发失效。',
    spring: 'AuthTokenInterceptor 先解析会话上下文，随后 VehicleController 调用 VehicleService 与 ImageService 组装 DTO，再把结构化响应返回给前端。',
    db: 'Service 层带着筛选条件查询 vehicle、vehicle_config、vehicle_image、image 等表并做关联映射，再把结果回传给 Spring 完成响应组装。',
    other: '图片元数据中的 objectName 会驱动 MinIO 读取对象字节流，最终通过后端返回浏览器完成图片渲染。'
  },
  {
    id: 'WF-02',
    title: '目录筛选链路',
    frontend: '上传页和筛选面板在级联选择时按层级携带 parentId、regionId、companyId、brandId 参数调用 /api/catalog/* 与 /api/regions|companies|brands|models，再把结果填入下拉项。',
    nginx: 'Nginx 对目录查询请求执行统一 /api 代理与限流后转发到 CatalogController 和主数据 Controller。',
    redis: '目录接口可选走短 TTL 缓存，命中时直接返回上次目录结果，未命中时回源 MySQL 查询后再写回缓存。',
    spring: 'CatalogController 会聚合 Region/Company/Brand/Model 服务层结果并输出轻量目录 DTO，让前端继续级联筛选。',
    db: 'MySQL 按 parent_id、region_id、brand_id、company_id 等索引查询主数据表并把记录返回给 Spring 聚合。',
    other: '该链路不依赖 MQ 与 ES，目录数据在一次同步请求内生成并返回。'
  },
  {
    id: 'WF-03',
    title: '认证链路',
    frontend: '登录、注册、找回密码和邮箱绑定表单会把 username、password、captcha、challengeId、emailCode 等字段提交到 /api/auth/*，并根据返回状态驱动下一步页面。',
    nginx: 'Nginx 对 /api/auth/* 应用 auth 专用限流桶后转发到 AuthController，保证认证入口抗刷。',
    redis: '认证服务把 session、captcha、otp、risk 计数和 reset ticket 写入 Redis 并附带 TTL，后续校验从这些键读取实时状态。',
    spring: 'AuthController 调用 AuthSecurityService、RateLimitService 与用户服务完成防刷校验、密码校验和会话签发，再返回 token、challenge 或失败码。',
    db: '注册、登录、改密和绑邮箱都会读取或更新 app_user 的密码哈希、邮箱、角色与状态字段，然后把持久化结果交回 Spring。',
    other: '发送邮箱验证码时 Spring Mail 会把验证码发到用户邮箱，用户再把收到的 code 回填到认证接口完成闭环。'
  },
  {
    id: 'WF-04',
    title: '用户互动链路',
    frontend: '用户中心、评论区和收藏按钮把 userId、vehicleId、page、size、content 参数发送到 /api/users*、/api/vehicles/{vehicleId}/comments、/api/favorites*，并即时刷新页面状态。',
    nginx: 'Nginx 把互动请求按 /api 规则限流并转发到 UserController、CommentController、FavoriteController。',
    redis: '评论列表、评论计数和收藏摘要优先读 Redis 短缓存，写操作后会删除或提升版本键，下一次读取再回源并重建缓存。',
    spring: 'RequireLogin 先校验登录态，随后服务层在事务中执行评论新增、收藏切换和用户资料聚合，并返回最新统计结果。',
    db: 'MySQL 读写 vehicle_comment、vehicle_favorite、app_user、image 表后把结果交回服务层并触发缓存更新。',
    other: '该链路采用同步事务提交，不经过消息队列，前端可以在一次响应里拿到最新互动状态。'
  },
  {
    id: 'WF-05',
    title: '上传链路',
    frontend: 'Upload 页把 file 与 payload 组装成 multipart 请求并可附加 Idempotency-Key，提交到 /api/upload 或 /api/images/upload。',
    nginx: 'Nginx 对上传入口启用 upload 限流、50M 请求体上限和 300s 超时后把请求转发到 UploadController/ImageController。',
    redis: 'RateLimitService 用 Redis 记录 userId/IP 频率，IdempotencyService 用 setIfAbsent 锁住 Idempotency-Key，从而决定放行还是直接拒绝重复请求。',
    spring: 'Spring 在接收 multipart 后由 UploadSecurityService 校验 MIME、魔数、尺寸和像素；随后 ImageService 按 busgallery.image-access.upload-* 配置执行原图压缩（质量/最大边）与水印写入，再调用 VehicleService、SubmissionService 落业务数据并返回 APPROVED/PENDING。',
    db: '上传通过后 MySQL 会写入 image、vehicle、vehicle_image、vehicle_submission 关系数据并把主键回传给服务层。',
    other: '处理后的图片二进制（含上传水印与压缩结果）会写入 MinIO 并返回 objectName/url，随后该元数据会入库供后续访问链路使用。'
  },
  {
    id: 'WF-06',
    title: '审核链路',
    frontend: '审核中心会把 submissionId、审批结果和驳回原因提交到 /api/reviews/inbox、/pending、/{id}/approve、/{id}/reject；车辆管理子页会调用 /api/vehicles/manage 与 /api/vehicles/{id}（查改删）维护车辆，省级审核范围会自动展开到省内市级车辆，保存后还会同步本地详情缓存。',
    nginx: 'Nginx 把审核请求统一转发到 ReviewController 并附带代理头，保持入口一致。',
    redis: '审核通过、审核页改车或删车后，服务层会递增车辆分页版本键，并删除对应车牌的 latest/version/stale/lock 快照键，确保其他页面下一次读取一定回源最新数据。',
    spring: 'RoleGuard 先校验 REVIEWER/STATION 权限与区域范围，然后服务层在事务里变更 submission 与关联实体并返回新状态。',
    db: 'MySQL 更新 vehicle_submission 状态及关联主表记录后把结果返回给 Spring 形成审核响应。',
    other: '审核链路当前无 MQ，状态变更在同一事务中生效并立即反馈前端。'
  },
  {
    id: 'WF-07',
    title: '后台管理链路',
    frontend: '后台页面把字典维护、角色调整、车辆和图片管理操作转换成 CRUD 请求发送到 /api/admin/* 与相关写接口；异常图片页会调用 /api/admin/images/suspects 与 /api/admin/images/suspects/cleanup 扫描并清理残留图片。',
    nginx: 'Nginx 对后台接口执行 /api 限流和反向代理后交给 AdminController 与对应业务 Controller。',
    redis: '后台涉及车辆写操作时会递增版本键或删除命中快照缓存，异常图片巡检本身不依赖 Redis，但清理后会让后续图片/车辆查询直接读取最新主数据。',
    spring: 'RequireLogin 与 RoleGuard 先做角色校验，随后服务层在事务中执行多表写入并返回更新后的实体或空响应。',
    db: 'MySQL 对 region、company、brand、model、vehicle、image 等主数据表执行增删改查并把结果持久化给查询链路复用。',
    other: '写链路抛出异常时事务会回滚，统一错误响应会返回前端避免部分成功。'
  },
  {
    id: 'WF-08',
    title: '图片签名访问链路',
    frontend: '列表和详情页拿到签名 token 后请求 /api/images/access/{token}，再把返回的字节流作为图片资源展示。',
    nginx: 'Nginx 先把签名访问请求转到后端验签接口，而不是直接暴露 MinIO 内网地址。',
    redis: '若请求附带会话 token，拦截器会读取 Redis 会话键补充用户上下文，但签名 token 本身不落 Redis。',
    spring: 'ImageAccessService 会解析 token 并校验 HMAC、过期时间和对象名合法性，校验通过后调用 MinIO 客户端拉取对象流。',
    db: '需要资源归属校验时服务层会读取 image 元数据判断可见性规则，再决定是否允许访问。',
    other: 'MinIO 在收到合法 objectName 后返回对象字节流，Spring 通过 ResponseEntity 把流写回浏览器。'
  },
  {
    id: 'WF-09',
    title: '快照链路',
    frontend: '首页热门模块和车牌详情页会提交 plateNumber 或 size 到 /api/snapshots/plate/{plateNumber} 与 /api/snapshots/hot，然后按快照字段一次性渲染。',
    nginx: 'Nginx 按常规 /api 规则转发快照请求到 SnapshotController。',
    redis: 'SnapshotService 先查 latest/version 键，未命中再查 stale 并使用 lock 防击穿；当车辆在审核页或后台被修改/删除后，对应车牌快照键会被主动清掉，下一次快照请求必定回源重建。',
    spring: 'SnapshotController 调用 SnapshotServiceImpl 聚合车辆、评论、收藏和推荐结果，序列化后写回 Redis 并响应 SnapshotPayload。',
    db: '回源时 MySQL 会按车牌查询 vehicle、image、comment、favorite 等表并把聚合结果交给 Service。',
    other: '快照中的图片字段会结合 MinIO 对象元数据补齐访问地址后再返回前端。'
  },
  {
    id: 'WF-10',
    title: '监控链路',
    frontend: '运维页面通过定时任务轮询 /api/metrics/db，并把 total、slow、samples 渲染到监控面板。',
    nginx: 'Nginx 对监控请求执行统一代理后转发到 DbMetricsController。',
    redis: '监控链路不走业务缓存，仅在存在登录态时可能读取会话上下文，其余数据直接由 Spring 聚合。',
    spring: 'DbMetricsController 从慢 SQL 采样组件汇总统计并输出 total/slow/samples 结构。',
    db: '采样器读取 SQL 执行记录并按慢查询阈值统计命中数量后返回给 Controller。',
    other: '监控 JSON 可被外部观测平台再次采集并驱动告警。'
  }
];
const flowTitleMap = Object.fromEntries(flowRows.map((x) => [x.id, x.title]));

const deployConfigs = [
  { key: 'DB_URL / DB_USERNAME / DB_PASSWORD', file: 'docker/.env', how: '将 DB_URL 指向 mysql:3306（容器）或 localhost:13306（本地），并替换账号密码', recommend: 'DB_URL=jdbc:mysql://mysql:3306/bus_gallery...; DB_USERNAME=root', effect: '后端数据库连接可用且时区一致' },
  { key: 'REDIS_HOST / REDIS_PORT / REDIS_PASSWORD', file: 'docker/.env + application.yml + compose', how: 'docker 环境用 redis:6379，本地环境改 localhost:6379，密码三处保持一致', recommend: 'REDIS_HOST=redis; REDIS_PORT=6379; REDIS_PASSWORD=12~16位强密码', effect: '会话、限流、缓存键读写正常' },
  { key: 'MINIO_ENDPOINT / MINIO_CDN_HOST / MINIO_BUCKET', file: 'docker/.env + application.yml', how: 'MINIO_ENDPOINT 固定 http://minio:9000，MINIO_CDN_HOST 改公网域名或反向代理地址', recommend: 'MINIO_CDN_HOST=https://img.example.com/bus-gallery; MINIO_BUCKET=bus-gallery', effect: '图片生成 URL 可被前端直接访问' },
  { key: 'AUTH_SECURITY_* / AUTH_RATE_*', file: 'docker/.env + application.yml', how: '按用户规模调验证码阈值和发送频率，防止误伤正常用户', recommend: 'CAPTCHA_TTL=180; LOGIN_CAPTCHA_FAILURE_THRESHOLD=5; SEND_CODE_IP_PER_DAY=200', effect: '认证安全与体验平衡' },
  { key: 'UPLOAD_SECURITY_*', file: 'docker/.env + application.yml', how: '按业务图片规格调整大小、像素和频率阈值', recommend: 'MAX_FILE_BYTES=15728640; USER_PER_MINUTE=20; GLOBAL_PER_MINUTE=300', effect: '上传链路稳定并降低恶意请求' },
  { key: 'IMAGE_ACCESS_UPLOAD_* / IMAGE_ACCESS_THUMBNAIL_WATERMARK_*', file: 'docker/.env + application.yml', how: '按展示需求开启原图水印、控制压缩质量与最大边长，缩略图水印可独立配置', recommend: 'UPLOAD_WATERMARK_ENABLED=true; UPLOAD_JPEG_QUALITY=0.82~0.9; UPLOAD_MAX_SIDE=2560', effect: '上传后自动水印与画质压缩，平衡可读性与带宽成本' },
  { key: 'limit_req / client_max_body_size / timeout', file: 'docker/nginx/default.conf', how: '根据并发和图片体积调整限流 burst、body 大小和超时', recommend: 'client_max_body_size 50M; proxy_read_timeout 300s', effect: '网关限流更稳，上传超时/413 更少' }
];

const envConfigExample = `# docker/.env
MYSQL_DATABASE=bus_gallery
DB_URL=jdbc:mysql://mysql:3306/bus_gallery?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=ChangeMe_Strong_123

REDIS_PASSWORD=ChangeMe_Redis_123
MINIO_ROOT_USER=admin
MINIO_ROOT_PASSWORD=ChangeMe_Minio_123
MINIO_BUCKET=bus-gallery
MINIO_CDN_HOST=http://localhost/bus-gallery

AUTH_SECURITY_CAPTCHA_ENABLED=true
AUTH_SECURITY_CAPTCHA_TTL_SECONDS=180
AUTH_SECURITY_CAPTCHA_CODE_LENGTH=4
AUTH_SECURITY_LOGIN_CAPTCHA_FAILURE_THRESHOLD=5
AUTH_RATE_SEND_CODE_IP_PER_DAY=200

UPLOAD_SECURITY_MAX_FILE_BYTES=15728640
UPLOAD_SECURITY_USER_PER_MINUTE=20
UPLOAD_SECURITY_GLOBAL_PER_MINUTE=300

IMAGE_ACCESS_UPLOAD_WATERMARK_ENABLED=true
IMAGE_ACCESS_UPLOAD_WATERMARK_TEXT=BUS GALLERY
IMAGE_ACCESS_UPLOAD_JPEG_QUALITY=0.86
IMAGE_ACCESS_UPLOAD_MAX_SIDE=2560`;

const appConfigExample = `# backend/src/main/resources/application.yml
spring:
  datasource:
    url: \${DB_URL:jdbc:mysql://localhost:3306/bus_gallery?...}
    username: \${DB_USERNAME:root}
    password: \${DB_PASSWORD:123456}
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
  data:
    redis:
      host: \${REDIS_HOST:redis}
      port: \${REDIS_PORT:6379}
      password: \${REDIS_PASSWORD:12345678}

minio:
  endpoint: \${MINIO_ENDPOINT:http://minio:9000}
  bucket: \${MINIO_BUCKET:bus-gallery}
  cdn-host: \${MINIO_CDN_HOST:}

auth:
  session:
    ttl-seconds: \${AUTH_SESSION_TTL_SECONDS:86400}
  security:
    captcha-ttl-seconds: \${AUTH_SECURITY_CAPTCHA_TTL_SECONDS:180}
    login-captcha-failure-threshold: \${AUTH_SECURITY_LOGIN_CAPTCHA_FAILURE_THRESHOLD:5}

busgallery:
  cache:
    vehicles:
      page-ttl-seconds: 30
  upload-security:
    max-file-bytes: \${UPLOAD_SECURITY_MAX_FILE_BYTES:15728640}
    upload-user-per-minute: \${UPLOAD_SECURITY_USER_PER_MINUTE:20}
  image-access:
    token-secret: \${IMAGE_ACCESS_TOKEN_SECRET:change-me-image-access-secret}
    full-ttl-seconds: \${IMAGE_ACCESS_FULL_TTL_SECONDS:300}
    thumbnail-watermark-enabled: \${IMAGE_ACCESS_THUMBNAIL_WATERMARK_ENABLED:true}
    thumbnail-watermark-text: \${IMAGE_ACCESS_THUMBNAIL_WATERMARK_TEXT:BUS GALLERY}
    upload-watermark-enabled: \${IMAGE_ACCESS_UPLOAD_WATERMARK_ENABLED:false}
    upload-watermark-text: \${IMAGE_ACCESS_UPLOAD_WATERMARK_TEXT:BUS GALLERY}
    upload-jpeg-quality: \${IMAGE_ACCESS_UPLOAD_JPEG_QUALITY:0.86}
    upload-max-side: \${IMAGE_ACCESS_UPLOAD_MAX_SIDE:2560}`;

const nginxConfigExample = `# docker/nginx/default.conf
limit_req_zone $binary_remote_addr zone=auth_ip:10m rate=12r/s;
limit_req_zone $binary_remote_addr zone=upload_ip:10m rate=6r/s;
limit_req_zone $binary_remote_addr zone=api_ip:10m rate=25r/s;

server {
  listen 80;
  client_max_body_size 50M;
  client_body_timeout 300s;

  location /api/upload {
    limit_req zone=upload_ip burst=12 nodelay;
    proxy_read_timeout 300s;
    proxy_send_timeout 300s;
    proxy_pass http://backend:8080;
  }

  location /api/auth/ {
    limit_req zone=auth_ip burst=30 nodelay;
    proxy_pass http://backend:8080;
  }

  location /api/ {
    limit_req zone=api_ip burst=40 nodelay;
    proxy_pass http://backend:8080;
  }
}`;
const apiConfigEntries = [
  { topic: '认证会话', file: 'backend/src/main/resources/application.yml', keys: 'auth.session.ttl-seconds, auth.security.*', effect: '控制 token 与验证码策略' },
  { topic: '网关限流', file: 'docker/nginx/default.conf', keys: 'limit_req_zone, limit_req', effect: '控制 auth/upload/api 入口限流' },
  { topic: 'Redis 连接', file: 'docker/.env + application.yml', keys: 'REDIS_HOST/PORT/PASSWORD', effect: '会话、幂等、缓存、限流能力' },
  { topic: '上传安全', file: 'application.yml', keys: 'busgallery.upload-security.*', effect: '文件尺寸、像素、频率阈值' },
  { topic: '图片签名+水印压缩', file: 'application.yml', keys: 'busgallery.image-access.token-secret, *.upload-*, *.thumbnail-watermark-*', effect: '控制访问签名、上传自动水印和压缩策略' }
];

const middlewarePages = [
  { pageId: 'm3-nginx', name: 'Nginx', intro: '统一入口网关，负责反向代理、限流、静态与对象代理。', role: '将 /api 请求转发到 backend，将 /bus-gallery 路径代理到 MinIO。', io: [{ input: 'HTTP 请求', process: 'location 匹配 + limit_req + proxy_pass', output: 'backend 响应或 429' }, { input: '/bus-gallery/*', process: '代理到 minio:9000', output: '对象流' }], configs: [{ key: 'limit_req_zone / limit_req', file: 'docker/nginx/default.conf', effect: '分层限流' }, { key: 'client_max_body_size 50M', file: 'docker/nginx/default.conf', effect: '上传体积上限' }, { key: 'proxy_read_timeout 300s', file: 'docker/nginx/default.conf', effect: '长请求超时控制' }], workflows: [{ flow: 'WF-03', input: '/api/auth/*', process: '认证接口限流+转发', output: '认证响应' }, { flow: 'WF-05', input: '/api/upload', process: '上传转发', output: '上传链路执行' }, { flow: 'WF-08', input: '/api/images/access/{token}', process: '先后端验签', output: '图片流' }] },
  { pageId: 'm3-redis', name: 'Redis', intro: '会话、限流、验证码、幂等、分页缓存、车牌快照与防击穿都依赖 Redis。', role: '读链路负责缓存与热点兜底，写链路负责版本推进、快照删键和幂等控制，是全站一致性最敏感的中间件之一。', io: [{ input: 'token/captcha/challenge/identity', process: '读写 key + TTL + 递增', output: '会话状态/限流判定' }, { input: 'plate 快照查询', process: 'latest/stale/lock 协同 + 主动删键', output: '命中或回源快照' }, { input: 'Idempotency-Key', process: 'setIfAbsent 防重复', output: '允许或拒绝提交' }], configs: [{ key: 'spring.data.redis.*', file: 'backend/src/main/resources/application.yml', effect: '连接参数' }, { key: 'appendonly yes', file: 'docker/redis/redis.conf', effect: 'AOF 持久化' }, { key: 'auth.session.ttl-seconds', file: 'application.yml', effect: '会话 TTL' }, { key: 'busgallery.cache.*', file: 'application.yml', effect: '业务缓存 TTL' }], workflows: [{ flow: 'WF-03', input: '登录与验证码请求', process: 'session/captcha/risk key 管理', output: '认证状态' }, { flow: 'WF-05', input: '上传请求', process: '限流+幂等', output: '允许上传或拒绝' }, { flow: 'WF-06', input: '审核页修改/删除车辆', process: '分页版本推进 + 快照删键', output: '其他页面回源最新数据' }, { flow: 'WF-09', input: '快照请求', process: '防击穿与缓存复用', output: '快照结果' }] },
  { pageId: 'm3-mysql', name: 'MySQL', intro: '承载用户、车辆、图片、评论、收藏、审核等核心持久化数据。', role: '通过 MyBatis/JPA 执行查询和事务写入。', io: [{ input: '业务请求参数', process: 'SQL 查询/更新', output: '实体与结果集' }, { input: '审核与后台写操作', process: '事务更新多表', output: '一致落库' }], configs: [{ key: 'spring.datasource.*', file: 'application.yml', effect: '连接地址和凭证' }, { key: 'spring.datasource.hikari.*', file: 'application.yml', effect: '连接池并发能力' }, { key: 'docker/init/init.sql', file: 'docker/init/init.sql', effect: '初始化表结构' }], workflows: [{ flow: 'WF-01', input: '车辆筛选条件', process: 'vehicle 相关表查询', output: '列表与详情' }, { flow: 'WF-06', input: '审核动作', process: 'submission 状态更新', output: '审核结果' }, { flow: 'WF-07', input: '后台 CRUD', process: '主数据维护', output: '最新主数据' }] },
  { pageId: 'm3-spring', name: 'Spring', intro: '鉴权拦截、控制器路由、服务层事务、异常处理均在 Spring 层。', role: 'WebMvcConfig 注入 AuthTokenInterceptor 到 /api/**，RoleGuard 做权限裁剪。', io: [{ input: 'HTTP 请求', process: 'Interceptor -> Controller -> Service -> Mapper', output: '统一响应' }, { input: 'Authorization/token', process: '会话解析并写入上下文', output: '用户上下文或 401' }], configs: [{ key: 'WebMvcConfig.addInterceptors', file: 'backend/config/WebMvcConfig.java', effect: '统一鉴权入口' }, { key: '@RequireLogin + RoleGuard', file: 'controller/auth', effect: '角色权限控制' }, { key: '@Transactional', file: 'service/controller', effect: '关键写链路事务一致性' }], workflows: [{ flow: 'WF-03', input: '认证请求', process: '认证/风控/会话签发', output: 'token' }, { flow: 'WF-05', input: '上传请求', process: '安全校验 + 幂等 + 水印压缩 + 业务写入', output: '上传结果' }, { flow: 'WF-10', input: '/api/metrics/db', process: '聚合慢 SQL 指标', output: '监控数据' }] },
  { pageId: 'm3-minio', name: 'MinIO', intro: '对象存储服务，负责图片数据落地与读取。', role: '上传写对象，访问通过后端签名校验后读取对象流。', io: [{ input: '上传文件', process: 'putObject 写入对象桶', output: 'objectName + url' }, { input: '签名 token', process: '验签后 getObject', output: '图片字节流' }], configs: [{ key: 'minio.endpoint/access-key/secret-key/bucket', file: 'application.yml + docker/.env', effect: '连接对象存储' }, { key: 'MINIO_CDN_HOST', file: 'docker/.env', effect: '外网访问域名' }, { key: 'location /bus-gallery/', file: 'docker/nginx/default.conf', effect: '统一对象访问入口' }], workflows: [{ flow: 'WF-05', input: '上传请求', process: '写对象并回写元数据', output: '可访问对象路径' }, { flow: 'WF-08', input: '图片访问请求', process: '验签后读取对象', output: '图片流' }] }
];
const middlewareMap = Object.fromEntries(middlewarePages.map((m) => [m.pageId, m]));
const middlewareEvents = [
  { trigger: '用户登录/注册', exec: 'Nginx -> Spring -> Redis -> MySQL', action: '限流、校验、写 session、返回 token', flow: 'WF-03', middlewarePages: ['m3-nginx', 'm3-spring', 'm3-redis', 'm3-mysql'] },
  { trigger: '请求车辆列表/详情', exec: 'Nginx -> Spring -> Redis -> MySQL -> MinIO', action: '缓存命中优先，未命中回源组装', flow: 'WF-01', middlewarePages: ['m3-nginx', 'm3-spring', 'm3-redis', 'm3-mysql', 'm3-minio'] },
  { trigger: '提交上传', exec: 'Nginx -> Spring -> Redis -> MinIO -> MySQL', action: '上传安全检查、幂等、防刷、图片压缩与水印处理后落库', flow: 'WF-05', middlewarePages: ['m3-nginx', 'm3-spring', 'm3-redis', 'm3-minio', 'm3-mysql'] },
  { trigger: '审核操作', exec: 'Nginx -> Spring -> MySQL -> Redis', action: '角色校验、状态更新、缓存失效', flow: 'WF-06', middlewarePages: ['m3-nginx', 'm3-spring', 'm3-mysql', 'm3-redis'] },
  { trigger: '访问签名图片', exec: 'Nginx -> Spring -> MinIO', action: '验签与过期校验后返回对象流', flow: 'WF-08', middlewarePages: ['m3-nginx', 'm3-spring', 'm3-minio'] }
];
const module5InterviewQa = [
  { id: 1, q: '这个系统的 SLO 是什么？', a: '当前版本重点监控上传成功率、鉴权成功率和核心查询延迟，工程目标是先保证核心接口稳定可用，再按压测结果逐步收紧到可量化 SLO（如上传成功率 > 99%、核心 GET 接口 P95 < 300ms）。' },
  { id: 2, q: '项目里最核心的不变量有哪些？', a: '核心不变量有三类：图片对象与元数据一一对应、审核状态迁移合法、角色权限边界不可越权。当前通过事务、RoleGuard 和签名访问策略来保障。' },
  { id: 3, q: '车辆/图片/审核单状态机如何设计？', a: '审核单在 pending/approved/rejected 间迁移，车辆与图片的最终可见状态由审核结果驱动。非法迁移由服务层在提交审核动作时拦截。' },
  { id: 4, q: '为什么删除车辆要先处理关联数据？', a: '因为 favorite/comment 等表对 vehicle 有外键约束，直接删主表会失败。当前做法是在事务里先清子表再删主表，保证数据完整性。' },
  { id: 5, q: '查询性能怎么保证？', a: '列表查询使用固定页大小+游标分页，减少深分页成本；并通过缓存键和版本失效机制降低重复查询压力。生产还应配合 explain 和慢 SQL 采样持续优化索引。' },
  { id: 6, q: '游标分页在并发写入下会不会乱序？', a: '游标由 launchDate+id 组成，能够保持稳定顺序并降低 offset 扫描成本。并发写入下可能出现“新数据插入到前页”的自然现象，前端通过刷新首屏解决。' },
  { id: 7, q: '哪些接口实现了幂等？', a: '上传链路支持 Idempotency-Key，后端用 Redis setIfAbsent 抢占键位避免重复写入。这样能覆盖用户重复点击和网络重试导致的重复提交。' },
  { id: 8, q: '对象存储与数据库双写如何一致？', a: '当前以服务层事务+失败回滚为主，核心目标是避免半成功；同时后台已提供异常图片巡检与清理入口，用于扫描无关联记录、原图缺失或缩略图缺失的残留数据。更严格场景仍建议补异步补偿任务。' },
  { id: 9, q: '图片压缩与水印放在同步链路会不会拖慢？', a: '当前是上传后同步处理，优势是结果一致、逻辑简单。高并发场景可拆为异步处理或增加专用处理队列，避免 CPU 密集逻辑挤占 API 线程。' },
  { id: 10, q: '压缩质量与尺寸参数如何确定？', a: '现在参数由配置驱动（如 upload-jpeg-quality、upload-max-side），先满足可读性与带宽平衡。上线后应基于样本图和终端网络数据做分层调优。' },
  { id: 11, q: '原图和缩略图权限是否一致？', a: '两者都走签名访问链路，但可分别配置水印策略。这样既保证访问安全，又允许在列表图和详情图采用不同防盗链强度。' },
  { id: 12, q: '签名 token 如何防重放和防篡改？', a: 'token 包含对象信息与过期时间并做 HMAC 校验，篡改或过期都会被拒绝。生产建议做密钥轮换并支持短窗口双密钥验证。' },
  { id: 13, q: '登录态和匿名访问的边界怎么划？', a: '写操作和管理接口必须登录；签名图片访问按 token 验签控制。边界由拦截器+注解约束，不依赖前端自行判断。' },
  { id: 14, q: '权限校验放在哪一层？', a: '入口由 Controller 注解和 RoleGuard 兜底，细粒度规则在 Service/业务方法内二次校验（如地区范围校验），避免单点绕过。' },
  { id: 15, q: '怎么证明没有越权？', a: '需要用角色矩阵做接口回归：匿名/普通用户/审核员/站长分别验证读写边界。当前设计已固化角色分层，建议补齐自动化权限测试。' },
  { id: 16, q: '缓存如何分层？', a: '车辆列表、快照等高频读采用 Redis 缓存；写操作后通过版本键或删键失效。这样能兼顾读性能和一致性。' },
  { id: 17, q: '写后读一致性怎么保证？', a: '关键写链路完成后主动失效相关缓存键，后续读取回源重建。比如审核页修改车辆会同步推进分页版本键并删除对应车牌快照键，因此首页、个人页和详情弹层不会长期停留在旧数据。' },
  { id: 18, q: 'Redis 故障会怎样？', a: '系统会损失会话、限流和缓存能力，核心接口可降级回源 DB 但压力会上升。生产应配高可用 Redis 和熔断降级策略。' },
  { id: 19, q: 'Nginx 限流和应用限流冲突如何定位？', a: '通过网关日志和后端错误码分层定位：网关拒绝通常直接 429，应用拒绝会带业务错误体。运维侧应建立统一链路日志字段。' },
  { id: 20, q: '你们看哪些业务指标？', a: '至少包括上传成功率、审核处理时延、图片访问 401/403 比例、车辆查询 P95、慢 SQL 比例。指标要和业务可用性直接关联。' },
  { id: 21, q: '有没有端到端可观测性？', a: '当前有慢 SQL 和接口日志基础能力，适合中小规模排障。进一步可接入 trace-id 贯穿 Nginx、Spring、Redis、MinIO、MySQL。' },
  { id: 22, q: '错误码体系怎么用于前端重试？', a: '401 引导登录恢复，429 引导退避重试，400 提示用户修正输入。保持稳定错误语义比“只看 message”更易治理。' },
  { id: 23, q: '历史脏数据怎么治理？', a: '当前已提供站长可见的异常图片巡检页，用来扫描无车辆关联、原图缺失、缩略图缺失的图片并支持一键清理。除此之外仍建议保留离线校验脚本，批量检查对象存在性、关联完整性和枚举合法性。' },
  { id: 24, q: '当前单点风险有哪些？', a: 'Redis、MySQL、MinIO 都是关键依赖；任一不可用都会影响核心流程。生产应至少做主从/集群、备份和故障演练。' },
  { id: 25, q: '流量增长 10 倍先扩哪里？', a: '先扩上传与图片处理能力（CPU 密集），再扩数据库读能力（索引+读写分离），最后优化缓存命中和热点治理。' },
  { id: 26, q: '测试覆盖怎么做才够？', a: '单测覆盖业务规则，集成测试覆盖事务和权限，端到端测试覆盖上传-审核-访问全链路。当前可运行后应优先补权限和上传异常路径测试。' },
  { id: 27, q: '文档和代码怎么防漂移？', a: '现在文档集中在 About.vue，维护成本较低但仍是人工同步。建议增加 API 扫描/契约校验，自动比对路由与文档条目。' },
  { id: 28, q: '安全防护做了哪些、还缺什么？', a: '已做登录鉴权、角色控制、限流、上传安全校验、签名访问。还可加强 WAF 规则、依赖漏洞扫描和审计日志完整性。' },
  { id: 29, q: '配置和密钥管理有什么要求？', a: '本地可用 .env，生产必须走密钥管理系统并做分环境隔离，禁止明文默认密钥。密钥变更要支持平滑切换和回滚。' },
  { id: 30, q: '现在如果上线，最担心什么？', a: '最担心三点：上传高峰下图片处理耗时、跨依赖双写一致性、权限边界回归覆盖不够。优先补压测、补偿任务和权限自动化回归。' }
];

const docsMainRef = ref(null);
const termGlossary = [
  { term: 'HTTP', note: '超文本传输协议，Web 请求的基础协议。' },
  { term: 'HTTPS', note: '加密版 HTTP，保证传输安全。' },
  { term: 'JWT', note: 'JSON Web Token，一种无状态令牌格式。' },
  { term: 'MinIO', note: '对象存储服务，用于保存图片对象。' },
  { term: 'SQL', note: '结构化查询语言，用于数据库读写。' },
  { term: 'CRUD', note: '增删改查四类基础数据操作。' },
  { term: 'DTO', note: '数据传输对象，用于接口层返回与接收。' },
  { term: 'EXIF', note: '图片元数据标准，包含拍摄参数等信息。' },
  { term: 'CDN', note: '内容分发网络，用于提升静态资源访问速度。' },
  { term: 'WAF', note: 'Web 应用防火墙，用于拦截恶意请求。' },
  { term: 'TTL', note: '生存时间，键或缓存条目的过期时长。' },
  { term: 'SLO', note: '服务等级目标，量化可用性和性能指标。' },
  { term: 'P95', note: '95 分位延迟，反映多数请求体验。' },
  { term: 'HMAC', note: '带密钥的消息摘要算法，用于签名防篡改。' },
  { term: 'MIME', note: '媒体类型标识，用于声明文件内容类型。' },
  { term: '幂等', note: '同一请求重复执行，多次结果与一次一致。' },
  { term: '限流', note: '限制单位时间请求量，防止系统被打垮。' },
  { term: '熔断', note: '下游异常时快速失败，避免故障扩散。' },
  { term: '回源', note: '缓存未命中后回到数据库或源服务查询。' },
  { term: '缓存一致性', note: '缓存与数据库在写后读场景保持可预期一致，常见做法是版本键或删键失效。' },
  { term: '权限边界', note: '不同角色可访问的数据和操作范围，必须在后端做强制校验。' },
  { term: '缓存击穿', note: '热点 key 失效瞬间大量请求回源，可用锁和 stale 数据缓解。' },
  { term: '防击穿', note: '防止热点 key 失效瞬间大量请求同时回源。' },
  { term: '游标分页', note: '基于游标而非 offset 的分页方式，适合大数据量。' },
  { term: '慢 SQL', note: '执行耗时较高的数据库语句，需优化。' },
  { term: '外键', note: '数据库表间约束，保证引用关系完整性。' },
  { term: '事务', note: '一组操作要么都成功要么都回滚。' },
  { term: '双写一致性', note: '跨两个存储写入时保证状态一致的能力。' }
];

const redisKeyRows = [
  { group: '会话', key: 'busgallery:sessions:{token}', value: 'UserSession JSON', ttl: 'auth.session.ttl-seconds (默认 86400s)', created: '登录成功 createSession', updated: '改昵称/角色时覆盖写；登出或强制下线删除', usage: '登录态校验、角色和审核地区读取' },
  { group: '会话', key: 'busgallery:user:sessions:{userId}', value: 'Set<token>', ttl: '与会话同 TTL', created: '登录后绑定 token 到用户', updated: '登出 remove；改密 deleteAllSessionsByUserId 清空', usage: '按用户批量踢线、广播会话更新' },
  { group: '验证码', key: 'busgallery:auth:captcha:{scene}:{captchaId}', value: 'CaptchaPayload JSON', ttl: 'auth.security.captcha-ttl-seconds (默认 180s)', created: 'issueCaptcha 生成图形验证码', updated: '输错次数递增并覆盖写；成功或超次删除', usage: '登录/找回密码的人机验证' },
  { group: '风控', key: 'busgallery:auth:risk:{scope}:{dimension}:{sha256(identity)}', value: 'count(number)', ttl: 'login-fail 30m / forgot-req 15m', created: '失败登录或找回触发 incrementRisk', updated: '持续累加；登录成功删除 login-fail 风险键', usage: '达到阈值后强制要求验证码' },
  { group: 'OTP', key: 'busgallery:auth:otp:{SCENE}:{challengeId}', value: 'OtpChallenge JSON', ttl: 'auth.security.otp-ttl-seconds (默认 300s)', created: '发送邮箱验证码 issueChallenge', updated: '输错 attempts 递增并覆盖写；成功校验删除', usage: '注册/改密/找回/绑邮箱校验' },
  { group: 'OTP', key: 'busgallery:auth:otp:cooldown:{scope}:{sha256(email)}', value: '"1"', ttl: 'auth.security.otp-resend-cooldown-seconds (默认 60s)', created: '发送验证码前 setIfAbsent', updated: 'TTL 到期自动消失', usage: '限制同邮箱短时间重复发码' },
  { group: 'OTP', key: 'busgallery:auth:ticket:reset:{ticket}', value: 'ResetTicketPayload JSON', ttl: 'auth.security.reset-ticket-ttl-seconds (默认 600s)', created: '找回密码验证码通过后签发 reset ticket', updated: 'consumeResetTicket 一次性消费后删除', usage: '找回密码二段式重置' },
  { group: '限流', key: 'busgallery:rl:{scope}:{dimension}:{sha256(identity)}:{slot}', value: 'counter(number)', ttl: 'window + 2s', created: 'RateLimitService.check 首次请求自动创建', updated: '窗口内自增；slot 变化后新键', usage: 'auth/login/upload 等维度限流' },
  { group: '幂等', key: 'idempotent:upload:{idempotencyKey}', value: '"1"', ttl: '上传接口固定 10m', created: 'UploadController 调用 runOnce 时 setIfAbsent', updated: '业务异常时主动删除；成功后等 TTL 过期', usage: '拦截重复上传提交' },
  { group: '车辆列表缓存', key: 'bg:vehicle:page:version', value: 'version(number)', ttl: '无固定 TTL', created: '首次写操作 increment 自动创建', updated: '车辆增改删、审核页修改/删除车辆后 increment', usage: '版本键，驱动 page 缓存整体失效' },
  { group: '车辆列表缓存', key: 'bg:vehicle:page:v{ver}:s{size}:r{r}:c{c}:b{b}:m{m}:k{kw}:l{lastLaunch}:i{lastId}', value: 'VehiclePageResponse JSON', ttl: 'busgallery.cache.vehicles.page-ttl-seconds (默认 30s)', created: '列表查询 miss 后回源写入', updated: '版本提升后自然失效，TTL 兜底过期', usage: '车辆分页查询加速；审核页保存后其他列表页会切到新版本命名空间' },
  { group: '评论缓存', key: 'bg:comments:ver:{vehicleId}', value: 'version(number)', ttl: '无固定 TTL', created: '首次评论写入 bumpVersion', updated: '每次新增评论 increment', usage: '评论 list/count 的版本失效锚点' },
  { group: '评论缓存', key: 'bg:comments:list:v{ver}:vid{vehicleId}:p{page}:s{size}', value: 'List<VehicleComment> JSON', ttl: 'busgallery.cache.comments.ttl-seconds (默认 30s)', created: '评论列表 miss 后回源写入', updated: '评论新增导致版本变化后自然失效', usage: '评论列表查询加速' },
  { group: '评论缓存', key: 'bg:comments:count:v{ver}:vid{vehicleId}', value: 'count(string)', ttl: 'busgallery.cache.comments.ttl-seconds (默认 30s)', created: '评论总数 miss 后回源写入', updated: '评论新增导致版本变化后自然失效', usage: '评论数量读取加速' },
  { group: '收藏缓存', key: 'bg:fav:summary:{vehicleId}', value: 'FavoriteSummary JSON', ttl: 'busgallery.cache.favorites.summary-ttl-seconds (默认 30s)', created: 'summary miss 或 toggle 后写入', updated: 'toggle 后覆盖最新总数和 topUsers', usage: '收藏摘要展示' },
  { group: '收藏缓存', key: 'bg:fav:liked:{vehicleId}:{userId}', value: '"true"/"false"', ttl: 'busgallery.cache.favorites.liked-ttl-seconds (默认 30s)', created: 'summary 查询当前用户收藏态时写入', updated: 'toggle 后覆盖写最新 liked', usage: '当前用户是否已收藏判定' },
  { group: '快照缓存', key: 'bg:snapshot:plate:{plate}:latest', value: 'version(string)', ttl: '10m', created: '快照回源构建后写入', updated: '每次重建快照覆盖写最新版本；车辆修改/删除时主动 delete', usage: '指向当前有效快照版本' },
  { group: '快照缓存', key: 'bg:snapshot:plate:{plate}:v{version}', value: 'base64(gzip(json))', ttl: '10m', created: '快照回源构建后写入版本键', updated: '新版本创建新 key；车辆修改/删除时命中版本键会被一起 delete', usage: '快照主缓存数据' },
  { group: '快照缓存', key: 'bg:snapshot:plate:{plate}:stale', value: 'base64(gzip(json))', ttl: '1h', created: '快照回源构建后同步写 stale', updated: '每次重建都覆盖 stale；车辆修改/删除时主动 delete', usage: '回源锁竞争时兜底返回旧快照' },
  { group: '快照缓存', key: 'bg:snapshot:plate:{plate}:lock', value: '"1"', ttl: '30s', created: '快照 miss 时 setIfAbsent 抢锁', updated: '持锁线程回源结束后 delete；车辆修改/删除时也会清理残余锁键', usage: '防止同车牌并发回源导致缓存击穿' }
];

const redisInterviewFlows = [
  {
    id: 'R1',
    title: '会话签发、校验与踢线',
    keys: 'busgallery:sessions:{token} / busgallery:user:sessions:{userId}',
    steps: [
      '登录成功后写会话 JSON + 用户 token 集合，两个 key 使用同一会话 TTL。',
      '每次请求由拦截器读取会话键解析角色和审核地区，权限边界在后续业务层继续校验。',
      '登出或改密时按 token 或 userId 批量删除会话，保证旧 token 立即失效。'
    ],
    mermaid: `sequenceDiagram
participant FE as Frontend
participant SPR as Spring(Auth)
participant RED as Redis
participant DB as MySQL
FE->>SPR: POST /api/auth/login
SPR->>DB: 校验用户名密码
DB-->>SPR: user
SPR->>RED: SET busgallery:sessions:{token} (TTL)
SPR->>RED: SADD busgallery:user:sessions:{userId}
SPR-->>FE: 返回 token
FE->>SPR: 携带 token 请求业务接口
SPR->>RED: GET busgallery:sessions:{token}
SPR-->>FE: 通过/拒绝`
  },
  {
    id: 'R2',
    title: '图形验证码与风险计数联动',
    keys: 'busgallery:auth:captcha:* / busgallery:auth:risk:*',
    steps: [
      '触发验证码后创建 captcha key，保存 codeHash/salt/ip/maxAttempts，避免明文 code 入库。',
      '登录失败或找回请求会累加 risk key，达到阈值后才强制校验验证码。',
      '验证码错误会递增 attempts，超限或校验成功后立即删键。'
    ],
    mermaid: `sequenceDiagram
participant FE as Frontend
participant SPR as Spring(Auth)
participant RED as Redis
FE->>SPR: GET /api/auth/captcha?scene=login
SPR->>RED: SET busgallery:auth:captcha:login:{captchaId}
SPR-->>FE: 返回 captchaId + 图片
FE->>SPR: POST /api/auth/login
SPR->>RED: INCR busgallery:auth:risk:login-fail:*
SPR->>RED: GET captcha key(阈值触发时)
SPR->>RED: DEL captcha key(成功或超次)`
  },
  {
    id: 'R3',
    title: '邮箱 OTP、冷却与重置票据',
    keys: 'busgallery:auth:otp:* / busgallery:auth:otp:cooldown:* / busgallery:auth:ticket:reset:*',
    steps: [
      '发送验证码先抢 cooldown 键，抢不到直接拒绝，防止邮箱通道被刷。',
      'OTP challenge 键只存 hash 和尝试次数，验证成功即删，失败则递增次数。',
      '找回密码流程中，OTP 校验通过后签发 reset ticket，重置时一次性消费删除。'
    ],
    mermaid: `sequenceDiagram
participant FE as Frontend
participant SPR as Spring(Auth)
participant RED as Redis
participant MQ as MailExecutor
FE->>SPR: 发送邮箱验证码
SPR->>RED: SETNX cooldown key (TTL 60s)
SPR->>RED: SET otp challenge key (TTL 300s)
SPR->>MQ: 异步发送邮件
FE->>SPR: 提交 OTP
SPR->>RED: GET otp challenge
SPR->>RED: DEL otp challenge(成功)
SPR->>RED: SET reset ticket(找回场景)`
  },
  {
    id: 'R4',
    title: '限流键的窗口计数',
    keys: 'busgallery:rl:{scope}:{dimension}:{sha256(identity)}:{slot}',
    steps: [
      '每个限流维度都按窗口计算 slot（分钟/小时），写入独立计数键。',
      '首次请求 INCR=1 时设置过期时间为 window+2s，避免边界抖动。',
      '超过阈值立即抛错并拒绝请求，新的 slot 会自动切换到新键。'
    ],
    mermaid: `sequenceDiagram
participant FE as Frontend
participant SPR as Spring(Service)
participant RED as Redis
FE->>SPR: 请求上传/登录接口
SPR->>RED: INCR busgallery:rl:*:{slot}
alt first hit
  SPR->>RED: EXPIRE key = window+2s
end
alt count > limit
  SPR-->>FE: 429/REQUEST_DUPLICATE
else pass
  SPR-->>FE: 继续业务处理
end`
  },
  {
    id: 'R5',
    title: '上传幂等键与异常回滚',
    keys: 'idempotent:upload:{idempotencyKey}',
    steps: [
      '上传入口读取 Idempotency-Key，执行 setIfAbsent 抢占幂等键。',
      '拿不到锁说明同 key 已处理或处理中，直接拒绝重复请求。',
      '业务异常时主动删键，避免错误请求长时间占坑；成功请求由 TTL 自然释放。'
    ],
    mermaid: `sequenceDiagram
participant FE as Frontend
participant SPR as UploadController
participant RED as Redis
participant DB as MySQL/MinIO
FE->>SPR: POST /api/upload (Idempotency-Key)
SPR->>RED: SETNX idempotent:upload:{key}
alt locked=false
  SPR-->>FE: Duplicate request
else locked=true
  SPR->>DB: 执行上传与落库
  alt exception
    SPR->>RED: DEL idempotent key
  end
  SPR-->>FE: 上传结果
end`
  },
  {
    id: 'R6',
    title: '车辆分页缓存与版本失效',
    keys: 'bg:vehicle:page:version / bg:vehicle:page:v{ver}:...',
    steps: [
      '读列表先读 version，再拼接参数形成 page key；命中直接返回。',
      '未命中回源 MySQL，写入 page 缓存并设置短 TTL。',
      '车辆增删改后只做 version 自增，不逐条删分页 key，降低并发失效开销。'
    ],
    mermaid: `sequenceDiagram
participant FE as Frontend
participant SPR as VehicleController
participant RED as Redis
participant DB as MySQL
FE->>SPR: GET /api/vehicles
SPR->>RED: GET bg:vehicle:page:version
SPR->>RED: GET bg:vehicle:page:v{ver}:...
alt hit
  RED-->>SPR: cached page
  SPR-->>FE: return cache
else miss
  SPR->>DB: query page
  SPR->>RED: SET page key + TTL
  SPR-->>FE: return db result
end`
  },
  {
    id: 'R7',
    title: '评论/收藏缓存的一致性策略',
    keys: 'bg:comments:* / bg:fav:*',
    steps: [
      '评论采用版本键模型：新增评论只 bump ver，list/count 缓存自然切换到新版本。',
      '收藏采用覆盖写模型：toggle 后直接回写 summary 与 liked 两类键。',
      '两种策略都以“写请求完成后立刻更新缓存状态”为核心，确保写后读一致性。'
    ],
    mermaid: `sequenceDiagram
participant FE as Frontend
participant SPR as Comment/Favorite Service
participant RED as Redis
participant DB as MySQL
FE->>SPR: 发布评论 / 切换收藏
SPR->>DB: insert/update
alt 评论
  SPR->>RED: INCR bg:comments:ver:{vehicleId}
else 收藏
  SPR->>RED: SET bg:fav:summary:{vehicleId}
  SPR->>RED: SET bg:fav:liked:{vehicleId}:{userId}
end
SPR-->>FE: 返回最新状态`
  },
  {
    id: 'R8',
    title: '快照缓存防击穿（latest + version + stale + lock）',
    keys: 'bg:snapshot:plate:{plate}:latest|v{version}|stale|lock',
    steps: [
      '先读 latest->version key，命中直接返回；未命中时先尝试读取 stale 兜底。',
      '只有抢到 lock 的请求允许回源构建快照，其余请求返回 stale 或等待下一次命中。',
      '回源完成后同时更新 version/latest/stale，再释放 lock，避免热点车牌并发打爆数据库。',
      '如果车辆在审核页或后台被修改/删除，对应车牌的 latest/version/stale/lock 会先被删掉，确保下一次请求不会继续命中旧快照。'
    ],
    mermaid: `sequenceDiagram
participant FE as Frontend
participant SPR as SnapshotService
participant RED as Redis
participant DB as MySQL
FE->>SPR: GET /api/snapshots/{plate}
SPR->>RED: GET latest + version key
alt miss
  SPR->>RED: GET stale
  SPR->>RED: SETNX lock (30s)
  alt lock acquired
    SPR->>DB: 回源聚合快照
    SPR->>RED: SET v{version} + latest (10m)
    SPR->>RED: SET stale (1h)
    SPR->>RED: DEL lock
  else lock denied
    SPR-->>FE: 返回 stale 或快速失败
  end
else hit
  SPR-->>FE: 直接返回缓存快照
end`
  },
  {
    id: 'R9',
    title: '审核页改车后的缓存刷新链路',
    keys: 'bg:vehicle:page:version / bg:snapshot:plate:{old|newPlate}:*',
    steps: [
      '审核页保存车辆后，后端先更新 MySQL，再推进分页版本键。',
      '如果车辆车牌未变，会删除该车牌的 latest/version/stale/lock；如果车牌变了，旧车牌和新车牌都会一起删。',
      '前端拿到最新 detail 后会同步 Vuex 的 detailMap 和当前 gallery 记录，避免当前单页里继续展示旧数据。',
      '其他页面下一次打开该车辆时会回源构建新快照，因此首页、个人页、详情弹层会看到最新结果。'
    ],
    mermaid: `sequenceDiagram
participant FE as ReviewCenter
participant SPR as VehicleService
participant RED as Redis
participant DB as MySQL
FE->>SPR: PUT /api/vehicles/{id}
SPR->>DB: 更新 vehicle / config / image
SPR->>RED: INCR bg:vehicle:page:version
SPR->>RED: DEL bg:snapshot:plate:{oldPlate}:latest|v{version}|stale|lock
SPR->>RED: DEL bg:snapshot:plate:{newPlate}:latest|v{version}|stale|lock
SPR-->>FE: 返回最新 detail
FE->>FE: 同步 detailMap / gallery`
  }
];

function redisActionHint(action) {
  const text = (action || '').toUpperCase();
  if (text.includes('SETNX')) return '尝试原子占位，成功才继续，失败走拒绝或兜底路径';
  if (text.includes('SET')) return '写入对应 Redis 键并同步状态（按场景设置 TTL）';
  if (text.includes('GET')) return '读取目标 Redis 键，按命中/未命中进入对应路径';
  if (text.includes('INCR')) return '原子计数，用于限流或版本推进';
  if (text.includes('DEL')) return '删除键，确保失效立即生效';
  if (text.includes('SADD')) return '维护集合关系，支持按用户聚合查询';
  if (text.includes('EXPIRE')) return '设置过期时间，避免脏键长期滞留';
  return '执行当前接口步骤并进入下一环节';
}

const redisSequenceToneTokens = {
  main: {
    stroke: '#2563eb',
    fill: '#f8fbff',
    border: '#93c5fd',
    chipFill: '#dbeafe',
    chipInk: '#1d4ed8',
    ink: '#1e3a8a',
    textFill: '#0f172a',
    hintFill: '#475569'
  },
  reply: {
    stroke: '#64748b',
    fill: '#f8fafc',
    border: '#cbd5e1',
    chipFill: '#e2e8f0',
    chipInk: '#334155',
    ink: '#334155',
    textFill: '#0f172a',
    hintFill: '#475569'
  },
  alt: {
    stroke: '#10b981',
    fill: '#f0fdf4',
    border: '#86efac',
    chipFill: '#d1fae5',
    chipInk: '#047857',
    ink: '#047857',
    textFill: '#14532d',
    hintFill: '#166534'
  },
  else: {
    stroke: '#f97316',
    fill: '#fff7ed',
    border: '#fdba74',
    chipFill: '#ffedd5',
    chipInk: '#c2410c',
    ink: '#c2410c',
    textFill: '#9a3412',
    hintFill: '#9a3412'
  }
};
const redisSequenceLaneThemes = [
  { bg: '#eef6ff', border: '#bfdbfe', ink: '#1d4ed8' },
  { bg: '#ecfeff', border: '#a5f3fc', ink: '#0e7490' },
  { bg: '#ecfdf5', border: '#a7f3d0', ink: '#047857' },
  { bg: '#fff7ed', border: '#fdba74', ink: '#c2410c' },
  { bg: '#f5f3ff', border: '#ddd6fe', ink: '#6d28d9' }
];

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function redisSequenceLaneTheme(index) {
  return redisSequenceLaneThemes[index % redisSequenceLaneThemes.length];
}

function redisSequenceTone(entry) {
  if (entry.type === 'branch') {
    if (entry.branchType === 'alt') return 'alt';
    if (entry.branchType === 'else') return 'else';
    return 'reply';
  }
  if (entry.arrow === '-->>') return 'reply';
  if ((entry.branchPath || '').includes('否则')) return 'else';
  if ((entry.branchPath || '').includes('如果')) return 'alt';
  return 'main';
}

function redisSequenceHint(entry) {
  const base = entry.hint || redisActionHint(entry.action || '');
  if (!entry.branchPath) return base;
  return `${base}，当前分支：${entry.branchPath}`;
}

function splitRedisSequenceText(text, maxChars, maxLines = 3) {
  let remaining = String(text || '').replace(/\s+/g, ' ').trim();
  if (!remaining) return [];
  const lines = [];
  while (remaining && lines.length < maxLines) {
    if (remaining.length <= maxChars) {
      lines.push(remaining);
      break;
    }
    let breakAt = maxChars;
    const slice = remaining.slice(0, maxChars + 1);
    const softBreak = Math.max(
      slice.lastIndexOf('，'),
      slice.lastIndexOf('。'),
      slice.lastIndexOf('；'),
      slice.lastIndexOf('：'),
      slice.lastIndexOf('、'),
      slice.lastIndexOf(','),
      slice.lastIndexOf('.'),
      slice.lastIndexOf(' '),
      slice.lastIndexOf(')')
    );
    if (softBreak >= Math.floor(maxChars * 0.55)) {
      breakAt = softBreak + 1;
    }
    const current = remaining.slice(0, breakAt).trim();
    if (current) {
      lines.push(current);
    }
    remaining = remaining.slice(breakAt).trim();
    if (lines.length === maxLines - 1 && remaining.length > maxChars) {
      lines.push(`${remaining.slice(0, Math.max(maxChars - 1, 1)).trim()}…`);
      return lines;
    }
  }
  return lines;
}

function splitRedisSequenceTextByWidth(text, maxWidth, maxLines = 3, fontSize = 11) {
  let remaining = String(text || '').replace(/\s+/g, ' ').trim();
  if (!remaining) return [];
  const lines = [];
  const softBreaks = new Set(['，', '。', '；', '：', '、', ',', '.', ' ', '/', ')', '）']);
  while (remaining && lines.length < maxLines) {
    if (redisSequenceTextWidth(remaining, fontSize) <= maxWidth) {
      lines.push(remaining);
      break;
    }
    let end = 0;
    let lastSoftBreak = -1;
    for (let idx = 0; idx < remaining.length; idx += 1) {
      const candidate = remaining.slice(0, idx + 1);
      if (redisSequenceTextWidth(candidate, fontSize) > maxWidth) {
        break;
      }
      end = idx + 1;
      if (softBreaks.has(remaining[idx])) {
        lastSoftBreak = idx + 1;
      }
    }
    if (end <= 0) {
      end = 1;
    }
    let breakAt = end;
    if (lastSoftBreak >= Math.floor(end * 0.55)) {
      breakAt = lastSoftBreak;
    }
    let current = remaining.slice(0, breakAt).trim();
    if (!current) {
      current = remaining.slice(0, end).trim();
      breakAt = end;
    }
    if (lines.length === maxLines - 1 && breakAt < remaining.length) {
      let truncated = current;
      while (truncated && redisSequenceTextWidth(`${truncated}…`, fontSize) > maxWidth) {
        truncated = truncated.slice(0, -1).trim();
      }
      lines.push(`${truncated || remaining.slice(0, 1)}…`);
      return lines;
    }
    lines.push(current);
    remaining = remaining.slice(breakAt).trim();
  }
  return lines;
}

function buildRedisSequenceArrowHead(x, y, direction) {
  if (direction === 'left') {
    return `${x},${y} ${x + 13},${y - 7} ${x + 13},${y + 7}`;
  }
  return `${x},${y} ${x - 13},${y - 7} ${x - 13},${y + 7}`;
}

function redisSequenceTextWidth(text, fontSize = 11) {
  const normalized = String(text || '');
  let units = 0;
  for (const ch of normalized) {
    units += /[A-Za-z0-9_.:/{}[\]|()-]/.test(ch) ? 0.62 : 1;
  }
  return Math.max(42, Math.ceil(units * fontSize + 14));
}

function maxRedisSequenceLineWidth(lines, fontSize) {
  if (!lines?.length) return 0;
  return Math.max(...lines.map((line) => redisSequenceTextWidth(line, fontSize)), 0);
}

function buildRedisSequenceDiagram(parsed, viewportWidth = 0) {
  const textSize = {
    laneLabel: 15,
    route: 15,
    action: 15,
    hint: 15,
    branchTitle: 15,
    branchTip: 15,
    tag: 15
  };
  const lineHeight = {
    laneLabel: 20,
    route: 20,
    action: 20,
    hint: 20,
    branchTitle: 20,
    branchTip: 20
  };
  const laneLabels = parsed?.lanes?.length ? parsed.lanes : ['Flow'];
  const laneCount = laneLabels.length;
  const minWidth = laneCount <= 2 ? 760 : laneCount === 3 ? 820 : 860;
  const requestedWidth = Number.isFinite(viewportWidth) ? viewportWidth : 0;
  let width = Math.max(minWidth, requestedWidth);
  const minSidePadding = laneCount <= 2 ? 124 : laneCount === 3 ? 120 : 112;
  const minLaneGap = laneCount <= 2 ? 252 : laneCount === 3 ? 222 : laneCount === 4 ? 192 : 174;
  let laneGap = laneCount <= 1 ? 0 : Math.max(minLaneGap, Math.floor((width - minSidePadding * 2) / (laneCount - 1)));
  width = Math.max(width, minSidePadding * 2 + (laneCount - 1) * laneGap);
  const sidePadding = laneCount <= 1 ? width / 2 : Math.round((width - (laneCount - 1) * laneGap) / 2);
  const laneCardWidth = clamp(Math.round((laneGap || 180) * 0.7), 160, 188);
  const laneLabelLines = laneLabels.map((label) => splitRedisSequenceTextByWidth(label, laneCardWidth - 14, 2, textSize.laneLabel));
  const laneCardHeight = laneLabelLines.some((lines) => lines.length > 1) ? 94 : 72;
  const laneCardY = 20;
  const headerHeight = laneCardY + laneCardHeight + 26;
  const laneBadgeWidth = 26;
  const laneBadgeHeight = 20;

  const lanes = laneLabels.map((label, index) => {
    const theme = redisSequenceLaneTheme(index);
    const x = sidePadding + index * laneGap;
    const labelLines = laneLabelLines[index];
    return {
      label,
      index,
      x,
      labelLines,
      cardX: x - laneCardWidth / 2,
      cardY: laneCardY,
      cardWidth: laneCardWidth,
      cardHeight: laneCardHeight,
      badgeX: x - laneBadgeWidth / 2,
      badgeY: laneCardY + 10,
      badgeWidth: laneBadgeWidth,
      badgeHeight: laneBadgeHeight,
      badgeTextY: laneCardY + 24,
      labelY: laneCardY + (labelLines.length > 1 ? 50 : 46),
      ...theme
    };
  });

  let cursorY = headerHeight + 14;
  const entries = (parsed?.events || []).map((entry, index) => {
    const tone = redisSequenceTone(entry);
    const toneToken = redisSequenceToneTokens[tone] || redisSequenceToneTokens.main;
    if (entry.type === 'branch') {
      const boxX = 52;
      const boxWidth = width - 104;
      const titleLines = splitRedisSequenceTextByWidth(entry.text, Math.max(180, boxWidth - 88), 2, textSize.branchTitle);
      const tipLines = splitRedisSequenceTextByWidth(entry.tip, Math.max(240, boxWidth - 36), 3, textSize.branchTip);
      const boxHeight = 26 + titleLines.length * lineHeight.branchTitle + 10 + tipLines.length * lineHeight.branchTip + 18;
      const stepBoxWidth = 30;
      const stepBoxHeight = 20;
      const branchEntry = {
        type: 'branch',
        step: index + 1,
        tone,
        detail: entry.pathHint || entry.tip,
        boxX,
        boxY: cursorY,
        boxWidth,
        boxHeight,
        stepBoxX: boxX + 12,
        stepBoxY: cursorY + 9,
        stepBoxWidth,
        stepBoxHeight,
        stepTextX: boxX + 12 + stepBoxWidth / 2,
        stepTextY: cursorY + 24,
        titleX: boxX + 58,
        titleY: cursorY + 30,
        titleLines,
        tipX: boxX + 18,
        tipY: cursorY + 30 + titleLines.length * lineHeight.branchTitle + 12,
        tipLines,
        fill: toneToken.fill,
        border: toneToken.border,
        chipFill: toneToken.chipFill,
        chipInk: toneToken.chipInk,
        textFill: toneToken.ink,
        hintFill: toneToken.hintFill
      };
      cursorY += boxHeight + 16;
      return branchEntry;
    }

    const action = normalizeAction(entry.action);
    const fromLane = lanes[entry.fromLane] || lanes[0];
    const toLane = lanes[entry.toLane] || lanes[0];
    const isSelf = entry.fromLane === entry.toLane;
    const fromX = fromLane.x;
    const toX = toLane.x;
    const spanWidth = Math.abs(toX - fromX);
    const availableSideWidth = Math.max(width - fromX - 28, fromX - 28);
    const maxCardWidth = isSelf
      ? clamp(Math.floor(Math.max(availableSideWidth - 12, 220)), 220, Math.min(width - 48, 460))
      : clamp(Math.floor(spanWidth + Math.min(180, laneGap * 0.72)), 220, Math.min(width - 48, 480));
    const baseCardWidth = isSelf
      ? clamp(Math.floor(Math.max((laneGap || 200) * 0.92, 220)), 220, maxCardWidth)
      : clamp(Math.floor(Math.max(spanWidth * 0.74, 220)), 220, maxCardWidth);
    const routeMaxWidth = Math.max(132, maxCardWidth - 72);
    const bodyMaxWidth = Math.max(172, maxCardWidth - 32);
    const routeLines = splitRedisSequenceTextByWidth(`${entry.from} ${entry.arrow === '-->>' ? '返回' : '调用'} ${entry.to}`, routeMaxWidth, 2, textSize.route);
    const hintLines = splitRedisSequenceTextByWidth(redisSequenceHint(entry), bodyMaxWidth, 3, textSize.hint);
    const actionLines = splitRedisSequenceTextByWidth(action, bodyMaxWidth, laneCount <= 3 ? 4 : 5, textSize.action);
    const branchTagText = entry.branchPath || '';
    const tagHeight = branchTagText ? 24 : 0;
    const rawTagWidth = branchTagText ? redisSequenceTextWidth(branchTagText, textSize.tag) : 0;
    const desiredCardWidth = Math.max(
      baseCardWidth,
      maxRedisSequenceLineWidth(routeLines, textSize.route) + 72,
      maxRedisSequenceLineWidth(actionLines, textSize.action) + 32,
      maxRedisSequenceLineWidth(hintLines, textSize.hint) + 32,
      branchTagText ? rawTagWidth + 24 : 0
    );
    const boxHeight = 18 + routeLines.length * lineHeight.route + 8 + actionLines.length * lineHeight.action + 8 + hintLines.length * lineHeight.hint + (tagHeight ? 30 : 0) + 16;
    let cardWidth = clamp(Math.ceil(desiredCardWidth), isSelf ? 220 : 220, maxCardWidth);
    let cardX = 24;
    let lineY = 0;
    let selfPath = '';
    let arrowHead = '';
    let rowHeight = 0;
    let connectorX = 0;

    if (isSelf) {
      const loopWidth = 52;
      const loopDepth = 42;
      const availableRight = width - fromX - 24;
      const availableLeft = fromX - 24;
      const preferRight = availableRight >= availableLeft;
      if (preferRight && availableRight >= cardWidth + 24) {
        cardX = fromX + 24;
      } else if (availableLeft >= cardWidth + 24) {
        cardX = fromX - cardWidth - 24;
      } else if (availableRight >= availableLeft) {
        cardX = width - cardWidth - 24;
      } else {
        cardX = 24;
      }
      cardX = clamp(cardX, 24, width - cardWidth - 24);
      const loopDirection = cardX >= fromX ? 1 : -1;
      lineY = cursorY + boxHeight + 20;
      selfPath = `M ${fromX} ${lineY} C ${fromX + loopDirection * loopWidth} ${lineY}, ${fromX + loopDirection * loopWidth} ${lineY + loopDepth}, ${fromX} ${lineY + loopDepth}`;
      arrowHead = buildRedisSequenceArrowHead(fromX, lineY + loopDepth, loopDirection > 0 ? 'left' : 'right');
      connectorX = clamp(fromX, cardX + 28, cardX + cardWidth - 28);
      rowHeight = boxHeight + loopDepth + 30;
    } else {
      const midpoint = (fromX + toX) / 2;
      cardX = clamp(midpoint - cardWidth / 2, 24, width - cardWidth - 24);
      lineY = cursorY + boxHeight + 20;
      arrowHead = buildRedisSequenceArrowHead(toX, lineY, toX < fromX ? 'left' : 'right');
      connectorX = clamp(midpoint, cardX + 28, cardX + cardWidth - 28);
      rowHeight = boxHeight + 36;
    }

    const stepBoxWidth = 30;
    const stepBoxHeight = 20;
    const textX = cardX + 16;
    const routeX = cardX + 58;
    const routeY = cursorY + 30;
    const actionY = routeY + routeLines.length * lineHeight.route + 8;
    const hintY = actionY + actionLines.length * lineHeight.action + 8;
    const tagWidth = branchTagText ? clamp(rawTagWidth, 60, cardWidth - 24) : 0;
    const messageEntry = {
      type: 'message',
      step: index + 1,
      tone,
      detail: buildRedisFlowDetail(entry),
      fromX,
      toX,
      lineY,
      cardX,
      cardY: cursorY,
      cardWidth,
      cardHeight: boxHeight,
      connectorX,
      routeX,
      routeY,
      routeLines,
      actionLines,
      actionY,
      hintLines,
      hintY,
      textX,
      stepBoxX: cardX + 12,
      stepBoxY: cursorY + 9,
      stepBoxWidth,
      stepBoxHeight,
      stepTextX: cardX + 12 + stepBoxWidth / 2,
      stepTextY: cursorY + 24,
      branchTagText,
      tagX: cardX + 12,
      tagY: cursorY + boxHeight - 30,
      tagWidth,
      tagHeight,
      tagTextX: cardX + 20,
      tagTextY: cursorY + boxHeight - 13,
      stroke: toneToken.stroke,
      fill: toneToken.fill,
      border: toneToken.border,
      chipFill: toneToken.chipFill,
      chipInk: toneToken.chipInk,
      ink: toneToken.ink,
      textFill: toneToken.textFill,
      hintFill: toneToken.hintFill,
      isReply: entry.arrow === '-->>',
      isSelf,
      selfPath,
      arrowHead
    };
    cursorY += rowHeight;
    return messageEntry;
  });

  const height = cursorY + 24;
  return {
    width,
    headerHeight,
    height,
    lanes: lanes.map((lane) => ({
      ...lane,
      lineY1: headerHeight - 6,
      lineY2: height - 24
    })),
    entries
  };
}

function buildBranchMeta(keyword, desc) {
  const normalized = normalizeBranchDesc(desc);
  if (keyword === 'alt') {
    return {
      branchType: 'alt',
      condition: normalized,
      text: '如果',
      pathLabel: normalized ? `如果：${normalized}` : '如果',
      tip: normalized || '满足当前判断条件',
      pathHint: normalized ? `进入“如果”分支，条件：${normalized}` : '进入“如果”分支，后续连线显示为绿色'
    };
  }
  if (keyword === 'else') {
    return {
      branchType: 'else',
      condition: normalized,
      text: '否则',
      pathLabel: normalized ? `否则：${normalized}` : '否则',
      tip: normalized || '不满足主判断条件',
      pathHint: normalized ? `进入“否则”分支，条件：${normalized}` : '进入“否则”分支，后续连线显示为橙色'
    };
  }
  return {
    branchType: 'end',
    text: '分支结束',
    pathLabel: '',
    tip: '两条分支在此汇合，后续回到主流程',
    pathHint: '分支汇合完成，后续连线恢复蓝色主流程'
  };
}

function normalizeBranchDesc(desc) {
  const raw = (desc || '').replace(/`/g, '').trim();
  if (!raw) return '';
  const lower = raw.toLowerCase();
  if (lower === 'first hit') return '当前窗口内是第一次命中计数键';
  if (lower === 'count > limit') return '计数值超过限流阈值';
  if (lower === 'pass') return '计数值未超过限流阈值';
  if (lower === 'locked=false') return '幂等键已被占用（重复请求）';
  if (lower === 'locked=true') return '幂等键占用成功（允许执行上传）';
  if (lower === 'exception') return '上传落库过程发生异常';
  if (lower === 'hit') return '缓存命中';
  if (lower === 'miss') return '缓存未命中';
  if (lower === 'lock acquired') return '成功获取快照回源锁';
  if (lower === 'lock denied') return '未获取到快照回源锁';
  return raw;
}

function normalizeAction(action) {
  return (action || '').replace(/`/g, '').trim();
}

function stripBranchPrefix(text, prefix) {
  return normalizeAction(text).replace(new RegExp(`^${prefix}[：:]?`), '').trim();
}

function redisActionTitle(action) {
  const text = normalizeAction(action);
  if (!text) return '执行步骤';
  if (text === 'user') return '返回用户记录';
  if (text.includes('校验用户名密码')) return '校验账号密码';
  if (text.includes('返回 token')) return '返回 token';
  if (text.includes('返回 captchaId + 图片')) return '返回验证码图片';
  if (text.includes('携带 token 请求业务接口')) return '携带 token 请求业务接口';
  if (text.includes('通过/拒绝')) return '返回鉴权结果';
  if (text.includes('发送邮箱验证码')) return '发送邮箱验证码';
  if (text.includes('提交 OTP')) return '提交 OTP';
  if (text.includes('请求上传/登录接口')) return '进入限流入口';
  if (text.includes('发布评论 / 切换收藏')) return '提交评论或收藏';
  if (text.includes('query page')) return '查询车辆分页';
  if (text.includes('cached page')) return '命中分页缓存';
  if (text.includes('return cache')) return '返回缓存数据';
  if (text.includes('return db result')) return '返回数据库结果';
  if (text.includes('回源聚合快照')) return '回源构建快照';
  if (text.includes('返回 stale')) return '返回 stale 快照';
  if (text.includes('直接返回缓存快照')) return '返回缓存快照';
  if (text.includes('返回最新 detail')) return '返回最新详情';
  if (text.includes('同步 detailMap / gallery')) return '同步前端详情缓存';
  if (/^GET\s+\/api\//i.test(text)) return `请求接口 ${text.replace(/^GET\s+/i, '')}`;
  if (/^POST\s+\/api\//i.test(text)) return `提交接口 ${text.replace(/^POST\s+/i, '')}`;
  if (/^PUT\s+\/api\//i.test(text)) return `更新接口 ${text.replace(/^PUT\s+/i, '')}`;
  if (/^DELETE\s+\/api\//i.test(text)) return `删除接口 ${text.replace(/^DELETE\s+/i, '')}`;
  if (/^GET\s+/i.test(text)) return '读取 Redis 键';
  if (/^SETNX\s+/i.test(text)) return '原子占位 Redis 键';
  if (/^SET\s+/i.test(text)) return '写入 Redis 键';
  if (/^INCR\s+/i.test(text)) return '递增 Redis 计数';
  if (/^DEL\s+/i.test(text)) return '删除 Redis 键';
  if (/^SADD\s+/i.test(text)) return '写入 Redis 集合';
  if (/^EXPIRE\s+/i.test(text)) return '设置键过期时间';
  return text;
}

function buildRedisFlowDetail(evt) {
  const action = normalizeAction(evt.action);
  if (!action) return `${evt.from} -> ${evt.to}：执行本步骤。`;
  if (action.includes('GET /api/auth/captcha?scene=login')) return '前端发起登录验证码请求，后端准备生成 captchaId 和图片内容，并写入验证码键供后续登录校验。';
  if (action.includes('POST /api/auth/login')) return '前端提交账号密码（及必要时验证码），后端依次做风控计数、验证码校验、密码校验和会话签发。';
  if (action.includes('POST /api/upload (Idempotency-Key)')) return '前端携带 Idempotency-Key 发起上传，后端先做幂等抢占，再进入上传压缩/水印/落库链路。';
  if (action.includes('GET /api/vehicles')) return '前端请求车辆分页列表，后端先读 version 与 page 缓存，命中走缓存，未命中再回源 MySQL。';
  if (action.includes('PUT /api/vehicles/{id}')) return '审核页或后台提交车辆修改请求，后端更新主数据后会推进分页版本，并清掉相关车牌快照键。';
  if (action.includes('GET /api/snapshots/{plate}')) return '前端请求车牌快照，后端按 latest/version/stale/lock 的顺序执行防击穿流程。';
  if (action.includes('携带 token 请求业务接口')) return '客户端携带 token 调业务接口，后端先读取会话键恢复用户身份，再按角色与审核地区做权限边界判定。';
  if (action.includes('发送邮箱验证码')) return '前端请求发送 OTP，后端先占用 cooldown 键，成功后写 challenge 键并异步发邮件。';
  if (action.includes('提交 OTP')) return '前端提交 challengeId+code，后端读取 OTP 键做哈希比对，成功后删除键并继续后续业务。';
  if (action.includes('请求上传/登录接口')) return '业务入口步骤：先执行限流计数和阈值判断，通过后才进入后续上传/登录业务。';
  if (action.includes('发布评论 / 切换收藏')) return '前端触发评论或收藏写操作，后端先更新 MySQL，再按业务类型更新 Redis 一致性键。';
  if (action === 'user') return 'MySQL 返回用户记录（含角色与审核地区）供后端签发会话并写入 Redis。';
  if (action.includes('返回 captchaId + 图片')) return '后端返回 captchaId 和验证码图片；前端后续登录请求必须携带该 captchaId 与输入值。';
  if (action.includes('返回 token')) return '会话写入 Redis 成功后返回 token 给前端；后续请求通过 token 命中 busgallery:sessions:{token} 维持登录态。';
  if (action.includes('通过/拒绝')) return '根据 busgallery:sessions:{token} 是否存在以及用户权限边界是否满足，决定请求放行或返回未登录/无权限。';
  if (action.includes('异步发送邮件')) return '后端把邮件任务投递到异步执行器，避免阻塞接口响应；OTP challenge 键在 Redis 中维持有效窗口。';
  if (action.includes('继续业务处理')) return '限流键未超阈值，流程进入接口核心逻辑；若是上传则继续幂等校验与落库。';
  if (action.includes('429/REQUEST_DUPLICATE')) return '限流计数超过阈值，立即返回 429 或重复请求错误，不再执行后续业务。';
  if (action.includes('Duplicate request')) return '幂等键已被占用，判定为重复提交，直接返回拒绝结果，避免重复写 MinIO/MySQL。';
  if (action.includes('上传结果')) return '上传链路完成后返回车辆/图片/审核状态等结果；成功路径保留幂等键至 TTL 到期。';
  if (action.includes('cached page')) return 'Redis 命中分页缓存键 bg:vehicle:page:v{ver}:...，直接返回缓存页数据。';
  if (action.includes('返回最新状态')) return '评论/收藏写入完成后返回最新聚合状态，前端无需二次查询即可展示最新计数与收藏态。';
  if (action.includes('GET busgallery:sessions:{token}')) return '从 Redis 读取会话键 busgallery:sessions:{token}，校验 token 是否有效并恢复用户角色/审核地区。';
  if (action.includes('SET busgallery:sessions:{token}')) return '向 Redis 写入会话键 busgallery:sessions:{token}（含 userId/role/reviewRegionId），并设置会话 TTL。';
  if (action.includes('SADD busgallery:user:sessions:{userId}')) return '把当前 token 放入集合键 busgallery:user:sessions:{userId}，用于后续按用户踢线/会话刷新。';
  if (action.includes('SET busgallery:auth:captcha:login:{captchaId}')) return '写入验证码键 busgallery:auth:captcha:login:{captchaId}，保存 codeHash、salt、尝试次数和过期时间。';
  if (action.includes('INCR busgallery:auth:risk:login-fail:*')) return '递增风控计数键 busgallery:auth:risk:login-fail:*，累计失败次数以决定是否强制验证码。';
  if (action.includes('GET captcha key')) return '读取验证码键 busgallery:auth:captcha:login:{captchaId}，校验验证码是否存在、是否过期、是否匹配。';
  if (action.includes('DEL captcha key')) return '删除验证码键 busgallery:auth:captcha:login:{captchaId}，防止同一验证码重复使用。';
  if (action.includes('SETNX cooldown key')) return '尝试占用冷却键 busgallery:auth:otp:cooldown:{scope}:{sha256(email)}，限制同邮箱短时间重复发码。';
  if (action.includes('SET otp challenge key')) return '写入 OTP 键 busgallery:auth:otp:{SCENE}:{challengeId}，保存 codeHash 和尝试次数。';
  if (action.includes('GET otp challenge')) return '读取 OTP 键 busgallery:auth:otp:{SCENE}:{challengeId}，核对邮箱验证码并判断是否过期。';
  if (action.includes('DEL otp challenge')) return '校验成功后删除 OTP 键 busgallery:auth:otp:{SCENE}:{challengeId}，确保验证码一次性使用。';
  if (action.includes('SET reset ticket')) return '写入重置票据键 busgallery:auth:ticket:reset:{ticket}，用于找回密码二段式确认。';
  if (action.includes('INCR busgallery:rl:*:{slot}')) return '递增限流键 busgallery:rl:{scope}:{dimension}:{sha256(identity)}:{slot}，按窗口统计请求次数。';
  if (action.includes('EXPIRE key = window+2s')) return '给当前限流键补上过期时间（窗口+2秒），避免脏计数长期残留。';
  if (action.includes('SETNX idempotent:upload:{key}')) return '尝试写入幂等键 idempotent:upload:{Idempotency-Key}，占位成功才允许进入上传业务。';
  if (action.includes('DEL idempotent key')) return '上传异常时删除幂等键 idempotent:upload:{Idempotency-Key}，避免错误占位导致后续请求被误拒绝。';
  if (action.includes('GET bg:vehicle:page:version')) return '读取分页版本键 bg:vehicle:page:version，确定本次查询应访问的缓存命名空间。';
  if (action.includes('GET bg:vehicle:page:v{ver}:...')) return '按筛选条件读取分页缓存键 bg:vehicle:page:v{ver}:s{size}:...，命中则直接返回列表。';
  if (action.includes('SET page key + TTL')) return '将查询结果写入分页缓存键 bg:vehicle:page:v{ver}:...，并设置 busgallery.cache.vehicles.page-ttl-seconds 过期时间。';
  if (action.includes('INCR bg:vehicle:page:version')) return '递增车辆分页版本键 bg:vehicle:page:version，让旧列表缓存整体退场；审核页改车后其他列表页会自动转向新版本。';
  if (action.includes('INCR bg:comments:ver:{vehicleId}')) return '评论写入后递增版本键 bg:comments:ver:{vehicleId}，使旧评论缓存自动失效。';
  if (action.includes('SET bg:fav:summary:{vehicleId}')) return '覆盖写收藏摘要键 bg:fav:summary:{vehicleId}，同步最新 total/topUsers。';
  if (action.includes('SET bg:fav:liked:{vehicleId}:{userId}')) return '覆盖写用户收藏态键 bg:fav:liked:{vehicleId}:{userId}，保证写后读立即一致。';
  if (action.includes('GET latest + version key')) return '读取快照索引键 bg:snapshot:plate:{plate}:latest，再读取版本键 bg:snapshot:plate:{plate}:v{version}。';
  if (action.includes('GET stale')) return '读取兜底键 bg:snapshot:plate:{plate}:stale，在高并发 miss 时返回最近一次可用快照。';
  if (action.includes('SETNX lock (30s)')) return '尝试占用回源锁 bg:snapshot:plate:{plate}:lock（30秒），只允许一个请求回源数据库。';
  if (action.includes('SET v{version} + latest (10m)')) return '回源后写入版本快照键和 latest 索引键（10分钟），让后续请求直接命中。';
  if (action.includes('SET stale (1h)')) return '同步更新 stale 键（1小时），作为回源锁争用时的兜底返回数据。';
  if (action.includes('DEL bg:snapshot:plate:{oldPlate}:latest|v{version}|stale|lock')) return '删除旧车牌快照相关键，避免改车牌或改车型后继续命中历史快照。';
  if (action.includes('DEL bg:snapshot:plate:{newPlate}:latest|v{version}|stale|lock')) return '删除新车牌当前命名空间下的快照键，确保下次打开详情时一定回源重建。';
  if (action.includes('DEL lock')) return '回源结束后删除锁键 bg:snapshot:plate:{plate}:lock，释放其他请求回源资格。';
  if (action.includes('校验用户名密码')) return '查询 MySQL 用户表并验证密码哈希；通过后进入会话签发步骤。';
  if (action.includes('执行上传与落库')) return '执行上传处理链路：图片校验/压缩/水印 -> MinIO 存储 -> MySQL 落库（vehicle、image、submission）。';
  if (action.includes('query page')) return '查询 MySQL vehicle 及关联表，按游标分页返回本页数据。';
  if (action.includes('回源聚合快照')) return '查询 MySQL 的 vehicle/image/comment/favorite 数据，组装成车牌快照对象。';
  if (action.includes('return cache')) return '直接返回 Redis 缓存结果，不再访问 MySQL。';
  if (action.includes('return db result')) return '返回 MySQL 回源结果，并由上游决定是否回填 Redis 缓存。';
  if (action.includes('返回 stale')) return '当前线程未拿到回源锁，返回 stale 快照避免并发打爆数据库。';
  if (action.includes('直接返回缓存快照')) return '最新快照已命中，直接返回缓存版本数据。';
  if (action.includes('返回最新 detail')) return '后端把最新车辆详情直接回给前端，前端可以立刻更新当前页面缓存而不用再二次查详情。';
  if (action.includes('同步 detailMap / gallery')) return '前端把刚保存成功的 detail 写回 Vuex 的 detailMap 和当前 gallery 记录，保证单页应用内的打开弹层立即看到新数据。';
  if (/^GET\s+/i.test(action)) {
    const key = action.replace(/^GET\s+/i, '').trim();
    return `读取 Redis 键或索引「${key}」，并按命中结果决定后续走缓存返回、回源构建或兜底分支。`;
  }
  if (/^SETNX\s+/i.test(action)) {
    const key = action.replace(/^SETNX\s+/i, '').trim();
    return `尝试原子占用 Redis 键「${key}」，占用成功才允许进入后续业务，不成功则进入拒绝或兜底分支。`;
  }
  if (/^SET\s+/i.test(action)) {
    const key = action.replace(/^SET\s+/i, '').trim();
    return `写入 Redis 键「${key}」，同步当前业务状态并按场景设置 TTL，保障后续读请求可直接命中。`;
  }
  if (/^INCR\s+/i.test(action)) {
    const key = action.replace(/^INCR\s+/i, '').trim();
    return `原子递增 Redis 计数键「${key}」，用于阈值判定或版本推进，计数结果直接决定分支走向。`;
  }
  if (/^DEL\s+/i.test(action)) {
    const key = action.replace(/^DEL\s+/i, '').trim();
    return `删除 Redis 键「${key}」，释放锁或回收一次性状态，防止脏状态影响后续请求。`;
  }
  if (/^EXPIRE\s+/i.test(action)) {
    const key = action.replace(/^EXPIRE\s+/i, '').trim();
    return `为 Redis 键设置过期策略（${key}），让状态在业务窗口后自动回收。`;
  }
  return `${evt.from} -> ${evt.to}：执行「${action}」，并进入下一步。`;
}

function parseMermaidSequence(mermaidText) {
  const lines = (mermaidText || '').split('\n').map((line) => line.trim()).filter(Boolean);
  const aliasToLabel = {};
  const lanes = [];
  const events = [];
  const branchStack = [];
  lines.forEach((line) => {
    const participantHit = line.match(/^participant\s+([A-Za-z0-9_]+)(?:\s+as\s+(.+))?$/i);
    if (participantHit) {
      const alias = participantHit[1];
      const label = (participantHit[2] || alias).trim();
      if (!aliasToLabel[alias]) {
        aliasToLabel[alias] = label;
        lanes.push(label);
      }
      return;
    }

    const messageHit = line.match(/^([A-Za-z0-9_]+)\s*(->>|-->>)\s*([A-Za-z0-9_]+)\s*:\s*(.+)$/);
    if (messageHit) {
      const fromAlias = messageHit[1];
      const toAlias = messageHit[3];
      events.push({
        type: 'message',
        from: aliasToLabel[fromAlias] || fromAlias,
        to: aliasToLabel[toAlias] || toAlias,
        arrow: messageHit[2],
        action: messageHit[4].trim(),
        hint: redisActionHint(messageHit[4].trim()),
        branchPath: branchStack.length ? branchStack[branchStack.length - 1].pathLabel : ''
      });
      const latest = events[events.length - 1];
      const fromLane = Math.max(0, lanes.indexOf(latest.from));
      const toLane = Math.max(0, lanes.indexOf(latest.to));
      latest.fromLane = fromLane;
      latest.toLane = toLane;
      return;
    }

    const branchHit = line.match(/^(alt|else|end)\s*(.*)$/i);
    if (branchHit) {
      const keyword = branchHit[1].toLowerCase();
      const desc = (branchHit[2] || '').trim();
      if (keyword === 'alt') {
        const meta = buildBranchMeta(keyword, desc);
        branchStack.push(meta);
        events.push({ type: 'branch', ...meta });
        return;
      }
      if (keyword === 'else') {
        const meta = buildBranchMeta(keyword, desc);
        if (branchStack.length) {
          branchStack[branchStack.length - 1] = meta;
        } else {
          branchStack.push(meta);
        }
        events.push({ type: 'branch', ...meta });
        return;
      }
      if (keyword === 'end') {
        const meta = buildBranchMeta(keyword, desc);
        events.push({ type: 'branch', ...meta });
        if (branchStack.length) {
          branchStack.pop();
        }
      }
    }
  });
  return { lanes, events };
}

const redisSequenceViewportWidth = ref(0);
let docsMainResizeObserver = null;

function syncRedisSequenceViewportWidth() {
  if (typeof window === 'undefined') return;
  const root = docsMainRef.value;
  if (!root) return;
  const styles = window.getComputedStyle(root);
  const paddingLeft = Number.parseFloat(styles.paddingLeft || '0') || 0;
  const paddingRight = Number.parseFloat(styles.paddingRight || '0') || 0;
  redisSequenceViewportWidth.value = Math.max(0, Math.floor(root.clientWidth - paddingLeft - paddingRight - 56));
}

const redisInterviewFlowViews = computed(() => redisInterviewFlows.map((item) => {
  const parsed = parseMermaidSequence(item.mermaid);
  return {
    ...item,
    parsed,
    diagram: buildRedisSequenceDiagram(parsed, redisSequenceViewportWidth.value)
  };
}));
const redisHeroStats = computed(() => ([
  { label: 'Key 模板', value: String(redisKeyRows.length), note: '当前文档列出的 Redis 键模板总数' },
  { label: '业务分层', value: String(new Set(redisKeyRows.map((row) => row.group)).size), note: '会话、限流、快照、分页等职责分区' },
  { label: '一致性策略', value: '4', note: '版本推进、主动删键、覆盖写、TTL 回收' },
  { label: '深挖流程', value: String(redisInterviewFlows.length), note: '把高频 Redis 问题拆成可讲清的流程图' }
]));
const redisOperationsOverview = [
  {
    title: '身份状态',
    badge: 'Session + OTP',
    desc: '登录态、图形验证码、邮箱验证码和找回票据都依赖 Redis 的短 TTL 状态机，目标是既能实时失效，又能支持风控和踢线。',
    key: 'busgallery:sessions:* / busgallery:auth:*',
    action: 'SET / GET / DEL / SADD',
    tone: 'teal'
  },
  {
    title: '入口保护',
    badge: 'Rate Limit',
    desc: '登录、上传等高风险入口通过窗口计数和幂等占位控制放行节奏，拦截恶意重试和重复提交。',
    key: 'busgallery:rl:* / idempotent:upload:*',
    action: 'INCR / EXPIRE / SETNX',
    tone: 'rose'
  },
  {
    title: '高频查询',
    badge: 'Page Cache',
    desc: '车辆分页、评论和收藏把高频读取从 DB 挪到 Redis，读多写少的场景优先命中缓存，写后靠版本或覆盖写保持一致。',
    key: 'bg:vehicle:* / bg:comments:* / bg:fav:*',
    action: 'GET / SET / INCR',
    tone: 'sky'
  },
  {
    title: '热点快照',
    badge: 'Snapshot',
    desc: '车牌快照走 latest + version + stale + lock 组合拳，兼顾热点命中、防击穿和写后主动失效。',
    key: 'bg:snapshot:plate:*',
    action: 'GET / SET / SETNX / DEL',
    tone: 'amber'
  }
];
const redisConsistencyCards = [
  {
    title: '列表版本推进',
    desc: '车辆增改删、审核页改车后只递增一个版本键，不去批量枚举删所有分页缓存。',
    trigger: '车辆新增、编辑、删除',
    key: 'bg:vehicle:page:version',
    result: '下一次列表查询切换到新命名空间',
    tone: 'sky'
  },
  {
    title: '快照主动删键',
    desc: '车牌相关缓存不等 TTL，写完立即删除 latest / version / stale / lock，防止首页和详情继续命中旧快照。',
    trigger: '审核页或后台修改/删除车辆',
    key: 'bg:snapshot:plate:{plate}:*',
    result: '下一次快照请求必定回源重建',
    tone: 'amber'
  },
  {
    title: '评论版本切换',
    desc: '评论列表和计数都挂在 version 后面，新增评论只 bump 一次版本键，老缓存自动退场。',
    trigger: '新增评论',
    key: 'bg:comments:ver:{vehicleId}',
    result: '评论 list/count 自动切到新版本',
    tone: 'cyan'
  },
  {
    title: '收藏覆盖写',
    desc: '收藏不是删缓存等回源，而是直接把 summary 和 liked 写成最新值，用户点完立刻看到新状态。',
    trigger: '切换收藏',
    key: 'bg:fav:summary:* / bg:fav:liked:*',
    result: '写后读立即一致',
    tone: 'emerald'
  }
];
const redisKeyGroupMeta = {
  '会话': { tone: 'teal', summary: '恢复用户身份、角色与审核地区' },
  '验证码': { tone: 'sky', summary: '图形验证码只在挑战窗口内有效' },
  '风控': { tone: 'rose', summary: '失败次数累加决定是否升级验证' },
  'OTP': { tone: 'violet', summary: '邮箱验证码与 reset ticket 都是一次性状态' },
  '限流': { tone: 'orange', summary: '按窗口计数，TTL 到期后自然回收' },
  '幂等': { tone: 'indigo', summary: '上传入口通过占位键拒绝重复提交' },
  '车辆列表缓存': { tone: 'sky', summary: 'version + page key 驱动分页缓存一致性' },
  '评论缓存': { tone: 'cyan', summary: '评论 list/count 通过版本键整体切换' },
  '收藏缓存': { tone: 'emerald', summary: 'toggle 后直接覆盖写回最新摘要' },
  '快照缓存': { tone: 'amber', summary: 'latest/version/stale/lock 负责热点快照与防击穿' }
};
const redisKeyGroupCards = computed(() => {
  const grouped = redisKeyRows.reduce((acc, row) => {
    if (!acc[row.group]) acc[row.group] = [];
    acc[row.group].push(row);
    return acc;
  }, {});
  return Object.keys(grouped).map((name) => {
    const meta = redisKeyGroupMeta[name] || { tone: 'sky', summary: 'Redis 键模板集合' };
    return {
      name,
      tone: meta.tone,
      summary: meta.summary,
      count: grouped[name].length,
      rows: grouped[name]
    };
  });
});
const redisArchiveOpen = ref(false);
const redisArchiveKeyword = ref('');
const redisArchiveGroup = ref('全部');
const redisArchiveGroups = computed(() => ['全部', ...new Set(redisKeyRows.map((row) => row.group))]);
const filteredRedisKeyRows = computed(() => {
  const keyword = redisArchiveKeyword.value.trim().toLowerCase();
  return redisKeyRows.filter((row) => {
    if (redisArchiveGroup.value !== '全部' && row.group !== redisArchiveGroup.value) {
      return false;
    }
    if (!keyword) {
      return true;
    }
    return [
      row.group,
      row.key,
      row.value,
      row.ttl,
      row.created,
      row.updated,
      row.usage
    ].some((field) => String(field || '').toLowerCase().includes(keyword));
  });
});
const redisArchivePreviewRows = computed(() => filteredRedisKeyRows.value.slice(0, 4));
const redisFlowDialog = reactive({
  visible: false,
  title: '',
  lanes: [],
  chart: {
    pre: [],
    yes: [],
    no: [],
    post: [],
    hasBranch: false,
    decisionText: '满足判断条件吗？',
    yesLabel: '如果（满足判断条件）',
    noLabel: '否则（不满足判断条件）'
  }
});

function buildRedisFlowStep(evt, index) {
  const action = normalizeAction(evt.action);
  return {
    title: `${index + 1}. ${redisActionTitle(action)}`,
    detail: buildRedisFlowDetail(evt)
  };
}

function buildRedisFlowChart(item) {
  const chart = {
    pre: [],
    yes: [],
    no: [],
    post: [],
    hasBranch: false,
    decisionText: '满足判断条件吗？',
    yesLabel: '如果（满足判断条件）',
    noLabel: '否则（不满足判断条件）'
  };
  if (!item || !item.parsed || !Array.isArray(item.parsed.events)) {
    return chart;
  }
  let zone = 'pre';
  item.parsed.events.forEach((evt, idx) => {
    if (evt.type === 'branch') {
      if (evt.branchType === 'alt') {
        chart.hasBranch = true;
        const altCond = evt.condition || stripBranchPrefix(evt.pathLabel || '', '如果') || '满足当前判断条件';
        chart.decisionText = altCond;
        chart.yesLabel = `如果：${altCond}`;
        chart.noLabel = `否则：未满足「${altCond}」`;
        zone = 'yes';
        return;
      }
      if (evt.branchType === 'else') {
        chart.hasBranch = true;
        const elseCond = evt.condition || stripBranchPrefix(evt.pathLabel || '', '否则');
        if (elseCond) {
          chart.noLabel = `否则：${elseCond}`;
        } else if (chart.decisionText && chart.decisionText !== '满足判断条件吗？') {
          chart.noLabel = `否则：未满足「${chart.decisionText}」`;
        } else {
          chart.noLabel = '否则';
        }
        zone = 'no';
        return;
      }
      if (evt.branchType === 'end') {
        zone = 'post';
      }
      return;
    }
    const step = buildRedisFlowStep(evt, idx);
    if (zone === 'yes') {
      chart.yes.push(step);
    } else if (zone === 'no') {
      chart.no.push(step);
    } else if (zone === 'post') {
      chart.post.push(step);
    } else {
      chart.pre.push(step);
    }
  });
  if (chart.hasBranch) {
    if (!chart.yes.length) chart.yes.push({ title: '无额外步骤', detail: '此分支直接进入合流节点' });
    if (!chart.no.length) chart.no.push({ title: '无额外步骤', detail: '此分支直接进入合流节点' });
  }
  return chart;
}

function openRedisFlowDialog(item) {
  redisFlowDialog.title = `${item.id} ${item.title}`;
  redisFlowDialog.lanes = item.parsed?.lanes || [];
  redisFlowDialog.chart = buildRedisFlowChart(item);
  redisFlowDialog.visible = true;
  if (typeof document !== 'undefined') {
    document.body.classList.add('redis-flow-modal-open');
  }
}

function closeRedisFlowDialog() {
  redisFlowDialog.visible = false;
  if (typeof document !== 'undefined') {
    document.body.classList.remove('redis-flow-modal-open');
  }
}
const termGlossaryWithLower = termGlossary.map((item) => ({
  ...item,
  lower: /[A-Za-z]/.test(item.term) ? item.term.toLowerCase() : item.term
}));

function isAsciiWordChar(ch) {
  return /[A-Za-z0-9_-]/.test(ch || '');
}

function findNextGlossaryMatch(text, fromIndex) {
  if (!text || fromIndex >= text.length) return null;
  const lowerText = text.toLowerCase();
  let best = null;
  for (const item of termGlossaryWithLower) {
    const isAscii = /[A-Za-z]/.test(item.term);
    let idx = (isAscii ? lowerText.indexOf(item.lower, fromIndex) : text.indexOf(item.term, fromIndex));
    while (idx !== -1) {
      if (isAscii) {
        const prev = idx > 0 ? text[idx - 1] : '';
        const next = idx + item.term.length < text.length ? text[idx + item.term.length] : '';
        if (isAsciiWordChar(prev) || isAsciiWordChar(next)) {
          idx = isAscii ? lowerText.indexOf(item.lower, idx + 1) : text.indexOf(item.term, idx + 1);
          continue;
        }
      }
      if (!best || idx < best.idx || (idx === best.idx && item.term.length > best.term.length)) {
        best = { idx, term: item.term, note: item.note };
      }
      break;
    }
  }
  return best;
}

function annotateTermTextNode(node) {
  const text = node.nodeValue || '';
  if (!text.trim()) return;
  let cursor = 0;
  let found = false;
  const frag = document.createDocumentFragment();
  while (cursor < text.length) {
    const hit = findNextGlossaryMatch(text, cursor);
    if (!hit) break;
    if (hit.idx > cursor) {
      frag.appendChild(document.createTextNode(text.slice(cursor, hit.idx)));
    }
    const span = document.createElement('span');
    span.className = 'term-note';
    span.title = hit.note;
    span.textContent = text.slice(hit.idx, hit.idx + hit.term.length);
    frag.appendChild(span);
    cursor = hit.idx + hit.term.length;
    found = true;
  }
  if (!found) return;
  if (cursor < text.length) {
    frag.appendChild(document.createTextNode(text.slice(cursor)));
  }
  node.parentNode?.replaceChild(frag, node);
}

function annotateDocTerms() {
  const root = docsMainRef.value;
  if (!root || typeof document === 'undefined') return;
  const walker = document.createTreeWalker(
    root,
    NodeFilter.SHOW_TEXT,
    {
      acceptNode(node) {
        if (!node || !node.nodeValue || !node.nodeValue.trim()) return NodeFilter.FILTER_REJECT;
        const parent = node.parentElement;
        if (!parent) return NodeFilter.FILTER_REJECT;
        if (parent.closest('pre, code, .code, .mono, .chip, .term-note')) return NodeFilter.FILTER_REJECT;
        return NodeFilter.FILTER_ACCEPT;
      }
    }
  );
  const textNodes = [];
  let current = walker.nextNode();
  while (current) {
    textNodes.push(current);
    current = walker.nextNode();
  }
  textNodes.forEach((node) => annotateTermTextNode(node));
}

const selectedPageId = ref('m1');
const module1Expanded = ref(false);
const module2Expanded = ref(false);
const apiTreeExpanded = ref(false);
const flowTreeExpanded = ref(false);
const module3Expanded = ref(false);
const activeLane = ref(-1);
const laneHover = reactive({ visible: false, x: 0, y: 0, text: '' });
const apiGroupExpanded = reactive({});

const rawApis = rawApiLines.trim().split('\n').map((x) => x.trim()).filter(Boolean).map((x) => {
  const [method, path] = x.split('|');
  return { method, path };
});
function groupOf(path) {
  if (path.includes('/vehicles/{vehicleId}/comments')) return 'Comment';
  if (path.startsWith('/api/auth')) return 'Auth';
  if (path.startsWith('/api/users')) return 'User';
  if (path.startsWith('/api/vehicles')) return 'Vehicle';
  if (path.startsWith('/api/favorites')) return 'Favorite';
  if (path.startsWith('/api/upload')) return 'Upload';
  if (path.startsWith('/api/images')) return 'Image';
  if (path.startsWith('/api/reviews')) return 'Review';
  if (path.startsWith('/api/admin')) return 'Admin';
  if (path.startsWith('/api/catalog')) return 'Catalog';
  if (path.startsWith('/api/regions')) return 'Region';
  if (path.startsWith('/api/companies')) return 'Company';
  if (path.startsWith('/api/brands')) return 'Brand';
  if (path.startsWith('/api/models')) return 'Model';
  if (path.startsWith('/api/snapshots')) return 'Snapshot';
  if (path.startsWith('/api/metrics')) return 'Metrics';
  return 'Other';
}

function flowOf(api) {
  if (api.group === 'Auth') return 'WF-03';
  if (['User', 'Comment', 'Favorite'].includes(api.group)) return 'WF-04';
  if (api.group === 'Upload') return 'WF-05';
  if (api.group === 'Review') return 'WF-06';
  if (api.group === 'Admin') return 'WF-07';
  if (api.group === 'Snapshot') return 'WF-09';
  if (api.group === 'Metrics') return 'WF-10';
  if (api.group === 'Image') return api.method === 'POST' ? 'WF-05' : api.method === 'GET' ? 'WF-08' : 'WF-07';
  if (api.group === 'Vehicle') {
    if (api.path === '/api/vehicles/manage') return 'WF-06';
    return api.method === 'GET' ? 'WF-01' : 'WF-07';
  }
  if (api.group === 'Catalog') return 'WF-02';
  if (['Region', 'Company', 'Brand', 'Model'].includes(api.group)) return api.method === 'GET' ? (api.path.endsWith('/vehicles') ? 'WF-01' : 'WF-02') : 'WF-07';
  return 'WF-02';
}

function cnName(api) {
  const m = api.method; const p = api.path; const g = api.group;
  if (g === 'Auth') {
    if (p.includes('/captcha')) return '获取图形验证码';
    if (p.includes('/register/send-email-code')) return '发送注册验证码';
    if (p.endsWith('/register')) return '注册账号';
    if (p.endsWith('/login')) return '用户登录';
    if (p.endsWith('/logout')) return '退出登录';
    if (p.includes('/password/change/send-email-code')) return '发送改密验证码';
    if (p.includes('/password/change/confirm')) return '确认修改密码';
    if (p.includes('/password/forgot/send-email-code')) return '发送找回验证码';
    if (p.includes('/password/forgot/verify-email-code')) return '校验找回验证码';
    if (p.includes('/password/forgot/reset')) return '重置密码';
    if (p.includes('/email/bind/send-email-code')) return '发送绑定邮箱验证码';
    if (p.includes('/email/bind/confirm')) return '确认绑定邮箱';
  }
  if (g === 'User') { if (p.endsWith('/me')) return '获取当前用户资料'; if (p.endsWith('/me/display-name')) return '修改用户昵称'; if (p.endsWith('/me/images')) return '获取我的图片'; if (p.includes('/images')) return '获取指定用户图片'; return '获取指定用户资料'; }
  if (g === 'Vehicle') { if (m === 'GET' && p === '/api/vehicles') return '查询车辆分页列表'; if (m === 'GET' && p === '/api/vehicles/manage') return '查询审核中心车辆管理列表'; if (m === 'GET' && p.includes('/plate/')) return '按车牌查询车辆分组'; if (m === 'GET') return '查询车辆详情'; if (m === 'POST') return '新增车辆'; if (m === 'PUT') return '更新车辆'; return '删除车辆'; }
  if (g === 'Comment') return m === 'GET' ? '查询车辆评论列表' : '发布车辆评论';
  if (g === 'Favorite') return p.endsWith('/toggle') ? '切换收藏状态' : p.endsWith('/summary') ? '查询收藏摘要' : '查询收藏列表';
  if (g === 'Upload') return '上传车辆与图片';
  if (g === 'Image') { if (p.includes('/access/')) return '按签名访问图片'; if (p.endsWith('/latest')) return '查询最新图片'; if (p.includes('/vehicle/')) return '查询车辆图片'; if (m === 'GET') return '查询图片详情'; if (m === 'POST') return '上传图片'; if (m === 'PUT') return '更新图片信息'; return '删除图片'; }
  if (g === 'Review') return p.endsWith('/inbox') ? '查询审核收件箱' : p.endsWith('/pending') ? '查询待审核列表' : p.includes('/approve') ? '通过审核' : p.includes('/reject') ? '驳回审核' : '更新审核提交';
  if (g === 'Admin') return p.endsWith('/overview') ? '查询后台概览' : p.endsWith('/users') ? '查询后台用户' : p.includes('/users/{id}/role') ? '调整用户角色' : p.endsWith('/submissions') ? '查询提交流水' : p.endsWith('/images/suspects') ? '查询异常图片' : p.endsWith('/images/suspects/cleanup') ? '清理异常图片' : m === 'GET' ? '查询字典数据' : m === 'POST' ? '新增字典数据' : m === 'PUT' ? '修改字典数据' : '删除字典数据';
  if (g === 'Catalog') return p.endsWith('/regions') ? '查询地区目录' : p.endsWith('/companies') ? '查询公司目录' : p.endsWith('/brands') ? '查询品牌目录' : '查询车型目录';
  if (g === 'Region') return m === 'GET' ? (p.endsWith('/companies') ? '查询地区下公司' : p.includes('{id}') ? '查询地区详情' : '查询地区列表') : m === 'POST' ? '新增地区' : m === 'PUT' ? '更新地区' : '删除地区';
  if (g === 'Company') return m === 'GET' ? (p.endsWith('/vehicles') ? '查询公司车辆' : p.endsWith('/model-summaries') ? '查询公司车型汇总' : p.includes('{id}') ? '查询公司详情' : '查询公司列表') : m === 'POST' ? '新增公司' : m === 'PUT' ? '更新公司' : '删除公司';
  if (g === 'Brand') return m === 'GET' ? (p.endsWith('/models') ? '查询品牌车型' : p.endsWith('/model-summaries') ? '查询品牌车型汇总' : p.includes('{id}') ? '查询品牌详情' : '查询品牌列表') : m === 'POST' ? '新增品牌' : m === 'PUT' ? '更新品牌' : '删除品牌';
  if (g === 'Model') return m === 'GET' ? (p.endsWith('/vehicles') ? '查询车型车辆' : p.endsWith('/company-summaries') ? '查询车型公司汇总' : p.includes('{id}') ? '查询车型详情' : '查询车型列表') : m === 'POST' ? '新增车型' : m === 'PUT' ? '更新车型' : '删除车型';
  if (g === 'Snapshot') return p.includes('/hot') ? '查询热门快照' : '按车牌查询快照';
  if (g === 'Metrics') return '查询数据库监控指标';
  return '接口';
}

function headersOf(api) {
  const p = api.path;
  if (p === '/api/vehicles/manage') return 'Authorization: Bearer <token> (REVIEWER/STATION)';
  if (p.startsWith('/api/admin')) return 'Authorization: Bearer <token> (STATION)';
  if (p.startsWith('/api/reviews')) return 'Authorization: Bearer <token> (REVIEWER/STATION)';
  if (p === '/api/upload') return 'Authorization: Bearer <token>, Idempotency-Key?';
  if (p === '/api/auth/logout' || p.includes('/password/change/') || p.includes('/email/bind/')) return 'Authorization: Bearer <token>';
  if (p === '/api/images/upload' || (p.startsWith('/api/images/{id}') && api.method !== 'GET')) return 'Authorization: Bearer <token>';
  if ((p.startsWith('/api/vehicles') && api.method !== 'GET') || (p.includes('/comments') && api.method === 'POST')) return 'Authorization: Bearer <token>';
  if (p.startsWith('/api/users/me')) return 'Authorization: Bearer <token>';
  if (p.startsWith('/api/favorites') && api.method !== 'GET') return 'Authorization: Bearer <token>';
  if (p === '/api/favorites') return 'Authorization: Bearer <token> (推荐)';
  return '-';
}

function pathFields(path) { return (path.match(/\{([^}]+)\}/g) || []).map((x) => x.slice(1, -1)); }
function sectionsOf(api) {
  const key = `${api.method} ${api.path}`;
  const path = pathFields(api.path); const query = []; const body = []; const multipart = [];
  if (key === 'GET /api/vehicles') query.push('size', 'regionId', 'companyId', 'brandId', 'modelId', 'keyword', 'lastLaunch', 'lastId');
  if (key === 'GET /api/vehicles/manage') query.push('size', 'regionId', 'companyId', 'brandId', 'modelId', 'keyword', 'lastLaunch', 'lastId');
  if (key === 'GET /api/users/me/images' || key === 'GET /api/users/{userId}/images' || key === 'GET /api/vehicles/{vehicleId}/comments') query.push('page', 'size');
  if (key === 'GET /api/favorites') query.push('userId?');
  if (key === 'GET /api/images/latest') query.push('limit');
  if (key === 'GET /api/admin/submissions') query.push('status');
  if (key === 'POST /api/admin/images/suspects/cleanup') body.push('imageIds?');
  if (key === 'GET /api/catalog/regions') query.push('parentId?');
  if (key === 'GET /api/catalog/companies') query.push('regionId?');
  if (key === 'GET /api/catalog/brands') query.push('companyId?');
  if (key === 'GET /api/catalog/models') query.push('brandId?', 'companyId?');
  if (key === 'GET /api/regions') query.push('parentId?', 'level?');
  if (key === 'GET /api/companies') query.push('regionId?');
  if (key === 'GET /api/models') query.push('brandId?');
  if (key === 'GET /api/snapshots/hot') query.push('size?');
  if (key === 'GET /api/auth/captcha') query.push('scene');
  if (key === 'POST /api/upload') multipart.push('file', 'payload');
  if (key === 'POST /api/images/upload') multipart.push('file', 'uploadUser?');
  if (key.includes('/api/auth/register/send-email-code')) body.push('username', 'email');
  if (key === 'POST /api/auth/register') body.push('username', 'displayName', 'password', 'confirmPassword', 'email', 'challengeId', 'emailCode');
  if (key === 'POST /api/auth/login') body.push('username', 'password', 'captchaId?', 'captchaCode?');
  if (key === 'POST /api/auth/password/change/send-email-code') body.push('currentPassword');
  if (key === 'POST /api/auth/password/change/confirm') body.push('challengeId', 'emailCode', 'newPassword', 'confirmPassword');
  if (key === 'POST /api/auth/password/forgot/send-email-code') body.push('usernameOrEmail', 'captchaId?', 'captchaCode?');
  if (key === 'POST /api/auth/password/forgot/verify-email-code') body.push('challengeId', 'emailCode');
  if (key === 'POST /api/auth/password/forgot/reset') body.push('resetTicket', 'newPassword', 'confirmPassword');
  if (key === 'POST /api/auth/email/bind/send-email-code') body.push('email', 'currentPassword');
  if (key === 'POST /api/auth/email/bind/confirm') body.push('challengeId', 'email', 'emailCode');
  if (key === 'PUT /api/users/me/display-name') body.push('displayName');
  if (key === 'POST /api/vehicles' || key === 'PUT /api/vehicles/{id}') body.push('plateNumber', 'brandId', 'modelId', 'companyId', 'regionId', 'launchDate', 'imageIds', 'config');
  if (key === 'POST /api/vehicles/{vehicleId}/comments') body.push('content');
  if (key === 'PUT /api/images/{id}') body.push('uploadUser', 'uploaderDisplayName?');
  if (key === 'POST /api/reviews/submissions/update') body.push('vehicleId', 'imageId', 'payload');
  if (key === 'POST /api/reviews/{id}/approve') body.push('payload?');
  if (key === 'POST /api/reviews/{id}/reject') body.push('rejectCode', 'rejectReason');
  if (key === 'PUT /api/admin/users/{id}/role') body.push('role', 'reviewRegionId?');
  if ((api.method === 'POST' || api.method === 'PUT') && body.length === 0 && multipart.length === 0) body.push('payload');
  return { path, query, body, multipart };
}

function valueOf(f) { const k = f.replace(/\?/g, ''); if (/id|page|size|limit|level/.test(k)) return 1; if (k.includes('password')) return 'P@ssw0rd!'; if (k.includes('email')) return 'demo@example.com'; if (k.includes('plate')) return '粤B12345'; if (k.includes('imageIds')) return [1001, 1002]; if (k.includes('config')) return { fuelType: 'diesel' }; if (k.includes('payload')) return { key: 'value' }; return 'sample'; }
function commentOf(f) { const k = f.replace(/\?/g, ''); if (k.includes('page')) return '分页页码'; if (k.includes('size')) return '分页大小'; if (k.toLowerCase().includes('id')) return '主键或关联 ID'; if (k.includes('token')) return '令牌'; if (k.includes('password')) return '密码字段'; if (k.includes('email')) return '邮箱'; if (k.includes('captcha')) return '图形验证码字段'; if (k.includes('payload')) return '业务负载'; return '业务参数'; }
function lit(v) { if (typeof v === 'number') return String(v); if (typeof v === 'object') return JSON.stringify(v); return `"${v}"`; }
function normalizeField(f) { return f.replace(/\?/g, ''); }
function requiredOfField(f) { return !f.includes('?'); }
function typeOfField(f, scope) {
  const k = normalizeField(f).toLowerCase();
  if (scope === 'multipart') return k === 'file' ? 'file' : 'string';
  if (k.includes('ids')) return 'array<number>';
  if (k.includes('config') || k.includes('payload')) return 'object';
  if (k.includes('date') || k.includes('time')) return 'string(datetime)';
  if (k.includes('password') || k.includes('token') || k.includes('code') || k.includes('email') || k.includes('name') || k.includes('content') || k.includes('plate') || k.includes('keyword')) return 'string';
  if (k.includes('id') || k.includes('page') || k.includes('size') || k.includes('limit') || k.includes('level')) return 'number';
  return scope === 'body' ? 'string|number|object' : 'string';
}
function exampleOfField(f) {
  const v = valueOf(f);
  return typeof v === 'object' ? JSON.stringify(v) : String(v);
}
function buildParamRows(sections) {
  const rows = { path: [], query: [], body: [], multipart: [] };
  ['path', 'query', 'body', 'multipart'].forEach((scope) => {
    sections[scope].forEach((name) => {
      rows[scope].push({
        name: normalizeField(name),
        type: typeOfField(name, scope),
        required: requiredOfField(name),
        desc: commentOf(name),
        example: exampleOfField(name)
      });
    });
  });
  return rows;
}
function paramJson(s) {
  if (!s.path.length && !s.query.length && !s.body.length && !s.multipart.length) return '{\n  // 无请求参数\n}';
  const ps = [['path', 'Path 参数', s.path], ['query', 'Query 参数', s.query], ['body', 'Body 参数', s.body], ['multipart', 'Multipart 参数', s.multipart]].filter((x) => x[2].length);
  const lines = ['{'];
  ps.forEach((p, i) => { lines.push(`  // ${p[1]}`); lines.push(`  "${p[0]}": {`); p[2].forEach((f, idx) => lines.push(`    "${f.replace(/\?/g, '')}": ${lit(valueOf(f))}${idx === p[2].length - 1 ? '' : ','} // ${commentOf(f)}`)); lines.push(`  }${i === ps.length - 1 ? '' : ','}`); });
  lines.push('}'); return lines.join('\n');
}
function reqExample(api) {
  const s = api.sections;
  const q = s.query.length ? '?' + s.query.map((x) => `${x.replace(/\?/g, '')}=${encodeURIComponent(String(valueOf(x)))}`).join('&') : '';
  const url = `http://localhost${api.path}${q}`;
  if (s.multipart.length) return `curl -X ${api.method} "${url}" \\\n  ${api.headers.includes('Authorization') ? '-H "Authorization: Bearer <token>" \\\n  ' : ''}${api.headers.includes('Idempotency-Key') ? '-H "Idempotency-Key: req-001" \\\n  ' : ''}-F "file=@bus.jpg" \\\n  -F "payload={\"plateNumber\":\"粤B12345\"};type=application/json"`;
  const body = s.body.reduce((a, f) => (a[f.replace(/\?/g, '')] = valueOf(f), a), {});
  return `curl -X ${api.method} "${url}"${api.headers.includes('Authorization') ? ' \\\n  -H "Authorization: Bearer <token>"' : ''}${s.body.length ? ` \\\n  -H "Content-Type: application/json" \\\n  -d '${JSON.stringify(body, null, 2)}'` : ''}`;
}
function successExample(api) { if (api.group === 'Auth' && api.path.includes('/captcha')) return JSON.stringify({ captchaId: 'cap_xxx', imageBase64: 'data:image/png;base64,...', expireInSeconds: 180 }, null, 2); if (api.group === 'Auth' && api.method === 'POST' && (api.path.endsWith('/login') || api.path.endsWith('/register'))) return JSON.stringify({ token: 'session_xxx', profile: { id: 1, username: 'demo', role: 'USER' } }, null, 2); if (api.group === 'Vehicle' && api.method === 'GET' && api.path === '/api/vehicles') return JSON.stringify({ records: [{ vehicle: { id: 101, plateNumber: '粤B12345' } }], total: 1, page: 1, size: 12 }, null, 2); if (api.group === 'Admin' && api.path.endsWith('/images/suspects')) return JSON.stringify([{ id: 301, issueSummary: 'UNLINKED, PRIMARY_MISSING', relationCount: 0, objectName: 'uploads/2026/03/demo.jpg', createTime: '2026-03-23T10:12:00' }], null, 2); if (api.group === 'Admin' && api.path.endsWith('/images/suspects/cleanup')) return JSON.stringify({ requestedCount: 3, deletedIds: [301, 302], failedIds: [303] }, null, 2); if (api.group === 'Image' && api.path.includes('/access/')) return 'HTTP/1.1 200 OK\nContent-Type: image/jpeg\n(binary stream)'; if (api.method === 'DELETE') return 'HTTP/1.1 200 OK\n(no response body)'; if (api.path.includes('/metrics/db')) return JSON.stringify({ total: 2331, slow: 13, samples: [{ sql: 'SELECT ...', ms: 211.2 }] }, null, 2); return JSON.stringify({ data: 'sample', success: true }, null, 2); }
function errorExample(api) { if (api.headers.includes('Authorization')) return JSON.stringify({ code: 'A0401', message: 'Unauthorized or session expired' }, null, 2); if (api.path.startsWith('/api/auth') || api.path === '/api/upload') return JSON.stringify({ code: 'A0429', message: '请求过于频繁，请稍后重试' }, null, 2); return JSON.stringify({ code: 'A0400', message: 'Invalid request parameter' }, null, 2); }

const configByGroup = {
  Auth: [{ key: 'auth.session.ttl-seconds', file: 'application.yml', how: '调整会话 TTL', effect: '影响登录有效期' }, { key: 'auth.security.*', file: 'application.yml', how: '调整验证码/风控阈值', effect: '影响认证安全策略' }],
  Vehicle: [{ key: 'busgallery.cache.vehicles.page-ttl-seconds', file: 'application.yml', how: '调整列表缓存 TTL', effect: '影响查询性能与实时性' }],
  Upload: [{ key: 'busgallery.upload-security.*', file: 'application.yml', how: '调整上传阈值', effect: '影响上传安全与体验' }, { key: 'busgallery.image-access.upload-*', file: 'application.yml', how: '配置上传自动水印与压缩参数', effect: '影响原图可读性、文件体积和带宽' }, { key: 'client_max_body_size', file: 'docker/nginx/default.conf', how: '调整体积上限', effect: '避免 413' }],
  Image: [{ key: 'busgallery.image-access.token-secret', file: 'application.yml', how: '生产环境替换默认密钥', effect: '影响签名安全性' }, { key: 'busgallery.image-access.thumbnail-watermark-*', file: 'application.yml', how: '配置缩略图水印开关与文案', effect: '影响列表图防盗链与品牌露出' }, { key: 'MINIO_CDN_HOST', file: 'docker/.env', how: '改公网域名', effect: '影响图片访问地址' }],
  Review: [{ key: 'RoleGuard', file: 'ReviewController + RoleGuard.java', how: '按权限模型调整', effect: '影响审核越权控制' }],
  Admin: [{ key: 'RoleGuard.requireStation()', file: 'AdminController.java', how: '控制后台权限入口', effect: '影响后台接口访问' }],
  Metrics: [{ key: 'busgallery.metrics.slow-query-ms', file: 'application.yml', how: '调整慢 SQL 阈值', effect: '影响监控采样灵敏度' }]
};
const apis = rawApis.map((r, idx) => {
  const group = groupOf(r.path); const flow = flowOf({ ...r, group }); const sections = sectionsOf({ ...r, group, flow });
  const paramRows = buildParamRows(sections);
  const base = { ...r, group, flow, pageId: `api-${String(idx + 1).padStart(3, '0')}`, groupName: groupMeta[group]?.name || group, cnName: cnName({ ...r, group }), headers: headersOf({ ...r, group }), sections };
  return { ...base, desc: `该接口用于${base.cnName}，归属“${base.groupName}”，链路 ${flow}。`, paramRows, paramJson: paramJson(sections), reqExample: reqExample(base), successExample: successExample(base), errorExample: errorExample(base), configHints: configByGroup[group] || [{ key: '-', file: '-', how: '无特定配置项', effect: '通用行为' }] };
});
const apiByPath = Object.fromEntries(apis.map((a) => [a.path, a]));
const apiGroups = computed(() => {
  const map = {}; apis.forEach((a) => { if (!map[a.group]) map[a.group] = []; map[a.group].push(a); });
  return groupOrder.filter((k) => map[k]).map((k) => ({ key: k, name: groupMeta[k].name, intro: groupMeta[k].intro, apis: map[k] }));
});
watch(apiGroups, (groups) => groups.forEach((g) => { if (apiGroupExpanded[g.key] === undefined) apiGroupExpanded[g.key] = false; }), { immediate: true });
const apiCategoryRows = computed(() => apiGroups.value.map((g) => ({ group: g.key, name: g.name, intro: g.intro, count: g.apis.length, firstPageId: g.apis[0].pageId })));
const apiParamSectionDefs = [
  { key: 'path', title: 'Path 参数' },
  { key: 'query', title: 'Query 参数' },
  { key: 'body', title: 'Body 参数' },
  { key: 'multipart', title: 'Multipart 参数' }
];
function apiParamSections(api) {
  if (!api) return [];
  return apiParamSectionDefs.filter((sec) => api.paramRows[sec.key].length).map((sec) => ({ ...sec, rows: api.paramRows[sec.key] }));
}
function apiStatusRows(api) {
  if (!api) return [];
  const rows = [{ code: '200', meaning: '请求成功', when: '业务处理成功并返回数据或空响应' }];
  if (api.headers.includes('Authorization')) rows.push({ code: '401', meaning: '未认证或会话失效', when: '缺少 token、token 过期、会话不存在' });
  if (api.path.startsWith('/api/auth') || api.path === '/api/upload' || api.path.includes('/images/upload')) rows.push({ code: '429', meaning: '请求频率超限', when: 'Nginx 或后端限流器命中阈值' });
  rows.push({ code: '400', meaning: '参数错误', when: '必填字段缺失、格式不合法、业务校验失败' });
  return rows;
}

const flowStepTags = {
  'WF-01': { frontend: ['参数组装'], nginx: ['入口限流'], redis: ['缓存一致性'], spring: ['拦截器顺序'], db: ['联合索引'], other: ['对象存储读取'] },
  'WF-02': { frontend: ['级联查询'], nginx: ['统一网关'], redis: ['短 TTL'], spring: ['聚合接口'], db: ['层级索引'], other: ['同步返回'] },
  'WF-03': { frontend: ['验证码场景'], nginx: ['认证限流'], redis: ['会话TTL'], spring: ['鉴权流程'], db: ['密码哈希'], other: ['邮件通道'] },
  'WF-04': { frontend: ['互动触发'], nginx: ['通用限流'], redis: ['热点缓存'], spring: ['登录校验'], db: ['事务写入'], other: ['同步反馈'] },
  'WF-05': { frontend: ['multipart'], nginx: ['上传通道'], redis: ['幂等+限流'], spring: ['文件安全校验', '水印压缩处理'], db: ['关系落库'], other: ['MinIO写入'] },
  'WF-06': { frontend: ['审批动作'], nginx: ['统一转发'], redis: ['缓存失效'], spring: ['角色校验'], db: ['状态变更'], other: ['即时生效'] },
  'WF-07': { frontend: ['后台CRUD'], nginx: ['入口保护'], redis: ['版本键'], spring: ['权限拦截'], db: ['主数据维护'], other: ['事务回滚'] },
  'WF-08': { frontend: ['签名访问'], nginx: ['先验签再取流'], redis: ['会话上下文'], spring: ['HMAC校验'], db: ['归属校验'], other: ['对象流回传'] },
  'WF-09': { frontend: ['快照请求'], nginx: ['常规代理'], redis: ['防击穿'], spring: ['聚合序列化'], db: ['回源聚合'], other: ['图片补齐'] },
  'WF-10': { frontend: ['定时轮询'], nginx: ['统一代理'], redis: ['非核心路径'], spring: ['指标聚合'], db: ['慢SQL统计'], other: ['告警采集'] }
};
const flowInterviewNotes = {
  'WF-01': [
    { tag: '缓存一致性', title: '为什么用版本键失效', explain: '写操作后只提升版本键而不是全量删 Key，能降低并发下误删和雪崩风险。' },
    { tag: '分页设计', title: '为什么用游标分页', explain: '按 launchDate + id 游标翻页能避免深分页 offset 扫描。' }
  ],
  'WF-02': [
    { tag: '目录缓存', title: '短 TTL 的价值', explain: '目录数据改动低频，短 TTL 能减少数据库读压力且不会长期陈旧。' }
  ],
  'WF-03': [
    { tag: '登录风控', title: '验证码触发阈值', explain: '连续失败达到阈值后再要求验证码，平衡首登体验和暴力破解防护。' },
    { tag: '会话方案', title: '为什么不是 JWT', explain: 'Redis Session 支持实时失效、踢人和风控状态共享，后台可控性更强。' }
  ],
  'WF-04': [
    { tag: '写后读一致', title: '互动数据如何更新', explain: '评论和收藏写入后立即删缓存，下一次读请求回源并重建。' }
  ],
  'WF-05': [
    { tag: '幂等', title: '重复提交如何挡住', explain: 'Idempotency-Key 使用 Redis setIfAbsent 抢占，重复请求直接返回已有结果。' },
    { tag: '上传安全', title: '为什么做魔数校验', explain: '仅靠 MIME 容易伪造，魔数校验可以阻断伪装可执行文件。' },
    { tag: '图片处理', title: '水印与压缩在何时执行', explain: '上传文件通过安全校验后，服务层会在入库前先按配置进行压缩与水印处理，再写对象存储和数据库。' }
  ],
  'WF-06': [
    { tag: '权限边界', title: '审核员区域隔离', explain: 'RoleGuard 先校验 REVIEWER 的区域范围，避免跨区域审批。' }
  ],
  'WF-07': [
    { tag: '后台一致性', title: '为什么要事务包裹', explain: '多表更新任一失败即回滚，防止字典和关联表状态不一致。' }
  ],
  'WF-08': [
    { tag: '签名安全', title: '防重放怎么做', explain: 'token 含过期时间并参与 HMAC，过期或篡改会直接拒绝访问。' }
  ],
  'WF-09': [
    { tag: '缓存击穿', title: 'lock + stale 的意义', explain: '并发 miss 时只有一个线程回源，其他线程读 stale 或等待，避免数据库被打爆。' }
  ],
  'WF-10': [
    { tag: '慢 SQL 监控', title: '阈值如何选', explain: '阈值过低会噪声过大，推荐先以 200ms 观察再按业务延迟目标调整。' }
  ]
};

const flowMainPathTemplates = {
  'WF-01': [
    { lane: 0, name: '前端发起查询' },
    { lane: 1, name: 'Nginx 转发' },
    { lane: 3, name: 'Spring 接收' },
    { lane: 2, name: 'Redis 读缓存' },
    { lane: 4, name: 'MySQL 回源' },
    { lane: 5, name: '对象流补齐' },
    { lane: 3, name: '服务组装响应' },
    { lane: 0, name: '前端渲染结果' }
  ],
  'WF-02': [
    { lane: 0, name: '前端级联请求' },
    { lane: 1, name: 'Nginx 转发' },
    { lane: 3, name: 'Spring 聚合目录' },
    { lane: 2, name: 'Redis 短缓存' },
    { lane: 4, name: 'MySQL 查询目录' },
    { lane: 3, name: '目录结果封装' },
    { lane: 0, name: '前端刷新选项' }
  ],
  'WF-03': [
    { lane: 0, name: '前端提交认证' },
    { lane: 1, name: 'Nginx 认证限流' },
    { lane: 3, name: 'Spring 风控校验' },
    { lane: 2, name: 'Redis 会话/验证码' },
    { lane: 4, name: 'MySQL 用户校验' },
    { lane: 5, name: '邮件服务发送码' },
    { lane: 3, name: '签发认证结果' },
    { lane: 0, name: '前端更新登录态' }
  ],
  'WF-04': [
    { lane: 0, name: '前端互动操作' },
    { lane: 1, name: 'Nginx 转发' },
    { lane: 3, name: 'Spring 鉴权处理' },
    { lane: 2, name: 'Redis 读写缓存' },
    { lane: 4, name: 'MySQL 持久化' },
    { lane: 3, name: '返回最新状态' },
    { lane: 0, name: '前端即时刷新' }
  ],
  'WF-05': [
    { lane: 0, name: '前端上传文件' },
    { lane: 1, name: 'Nginx 上传入口' },
    { lane: 3, name: 'Spring 安全校验' },
    { lane: 2, name: 'Redis 幂等限流' },
    { lane: 3, name: 'Spring 水印压缩处理' },
    { lane: 5, name: '对象存储写入' },
    { lane: 4, name: 'MySQL 写元数据' },
    { lane: 3, name: '返回审核结果' },
    { lane: 0, name: '前端展示状态' }
  ],
  'WF-06': [
    { lane: 0, name: '前端提交审核' },
    { lane: 1, name: 'Nginx 转发' },
    { lane: 3, name: 'Spring 权限校验' },
    { lane: 4, name: 'MySQL 更新状态' },
    { lane: 2, name: 'Redis 失效缓存' },
    { lane: 3, name: '返回审核结果' },
    { lane: 0, name: '前端更新列表' }
  ],
  'WF-07': [
    { lane: 0, name: '前端后台写操作' },
    { lane: 1, name: 'Nginx 转发' },
    { lane: 3, name: 'Spring 权限+事务' },
    { lane: 4, name: 'MySQL 更新主数据' },
    { lane: 2, name: 'Redis 版本失效' },
    { lane: 3, name: '返回变更结果' },
    { lane: 0, name: '前端刷新管理页' }
  ],
  'WF-08': [
    { lane: 0, name: '前端请求签名图' },
    { lane: 1, name: 'Nginx 转后端验签' },
    { lane: 3, name: 'Spring 校验 token' },
    { lane: 2, name: 'Redis 会话上下文' },
    { lane: 4, name: 'MySQL 归属校验' },
    { lane: 5, name: '对象存储取流' },
    { lane: 3, name: '流式响应' },
    { lane: 0, name: '前端展示图片' }
  ],
  'WF-09': [
    { lane: 0, name: '前端请求快照' },
    { lane: 1, name: 'Nginx 转发' },
    { lane: 3, name: 'Spring 快照服务' },
    { lane: 2, name: 'Redis latest/stale' },
    { lane: 4, name: 'MySQL 回源聚合' },
    { lane: 5, name: '对象信息补齐' },
    { lane: 3, name: '写回缓存并响应' },
    { lane: 0, name: '前端一次渲染' }
  ],
  'WF-10': [
    { lane: 0, name: '前端轮询指标' },
    { lane: 1, name: 'Nginx 转发' },
    { lane: 3, name: 'Spring 聚合指标' },
    { lane: 4, name: 'MySQL 采样统计' },
    { lane: 3, name: '返回监控数据' },
    { lane: 0, name: '前端刷新监控图' }
  ]
};

const flowPages = computed(() => flowRows.map((f) => {
  const rel = apis.filter((a) => a.flow === f.id);
  const firstApi = rel[0];
  const tags = flowStepTags[f.id] || {};
  const commonApis = rel.slice(0, 3);
  const swimlanes = ['浏览器/前端', 'Nginx', 'Redis', 'Spring', 'MySQL', 'MinIO/外部服务'];
  const sequence = [
    {
      name: '前端发起请求',
      text: f.frontend,
      targetPage: firstApi ? firstApi.pageId : 'm2-intro',
      targetText: firstApi ? `查看 API：${firstApi.cnName}` : '查看接口总览',
      tags: tags.frontend || [],
      apis: commonApis
    },
    {
      name: 'Nginx 入口',
      text: f.nginx,
      targetPage: 'm3-nginx',
      targetText: '查看 Nginx 页面',
      tags: tags.nginx || [],
      apis: commonApis
    },
    {
      name: 'Redis',
      text: f.redis,
      targetPage: 'm3-redis',
      targetText: '查看 Redis 页面',
      tags: tags.redis || [],
      apis: commonApis
    },
    {
      name: 'Spring 服务层',
      text: f.spring,
      targetPage: 'm3-spring',
      targetText: '查看 Spring 页面',
      tags: tags.spring || [],
      apis: commonApis
    },
    {
      name: 'MySQL',
      text: f.db,
      targetPage: 'm3-mysql',
      targetText: '查看 MySQL 页面',
      tags: tags.db || [],
      apis: commonApis
    },
    {
      name: '其他中间件',
      text: f.other,
      targetPage: 'm3-minio',
      targetText: '查看 MinIO 页面',
      tags: tags.other || [],
      apis: commonApis
    },
    {
      name: '后端回传结果',
      text: 'Spring 将聚合后的业务结果编码为 JSON（或图片字节流）返回给前端，前端再根据响应类型完成页面渲染与状态更新。',
      targetPage: firstApi ? firstApi.pageId : 'm2-intro',
      targetText: '回到相关 API',
      tags: ['响应封装', '前端渲染'],
      apis: commonApis
    }
  ];
  const events = [
    { from: 0, to: 1, title: '前端提交业务请求', ...sequence[0] },
    { from: 1, to: 3, title: '网关转发到 Spring', ...sequence[1] },
    { from: 3, to: 3, title: 'Spring 执行业务编排', ...sequence[3] },
    { from: 3, to: 2, title: 'Spring 访问 Redis', ...sequence[2] },
    { from: 3, to: 4, title: 'Spring 回源 MySQL', ...sequence[4] },
    { from: 3, to: 5, title: 'Spring 调用对象/外部服务', ...sequence[5] },
    { from: 3, to: 0, title: 'Spring 返回响应给前端', ...sequence[6] }
  ];
  const laneTargetPage = (lane) => {
    if (lane === 0) return firstApi ? firstApi.pageId : 'm2-intro';
    if (lane === 1) return 'm3-nginx';
    if (lane === 2) return 'm3-redis';
    if (lane === 3) return 'm3-spring';
    if (lane === 4) return 'm3-mysql';
    return 'm3-minio';
  };
  const mainPath = (flowMainPathTemplates[f.id] || [
    { lane: 0, name: '前端发起请求' },
    { lane: 1, name: 'Nginx 转发' },
    { lane: 3, name: 'Spring 处理' },
    { lane: 4, name: 'MySQL 读写' },
    { lane: 3, name: '返回响应' },
    { lane: 0, name: '前端渲染' }
  ]).map((node) => ({ ...node, targetPage: laneTargetPage(node.lane) }));
  return {
    ...f,
    pageId: 'flow-' + f.id.toLowerCase(),
    shortTitle: f.title.length > 14 ? f.title.slice(0, 14) + '...' : f.title,
    intro: `该页以横向时序图展示 ${f.id} 的数据在前端、网关、缓存、服务和存储之间如何流动。`,
    sequence,
    swimlanes,
    events,
    mainPath,
    layerFlow: [
      { name: '前端层', text: f.frontend },
      { name: '网关层', text: f.nginx },
      { name: '缓存层', text: f.redis },
      { name: '服务层', text: f.spring },
      { name: '数据层', text: f.db },
      { name: '对象/外部层', text: f.other }
    ],
    notes: flowInterviewNotes[f.id] || []
  };
}));

const isModule1Page = computed(() => selectedPageId.value === 'm1' || selectedPageId.value === 'm1-config');
const isModule2Page = computed(() => selectedPageId.value === 'm2-intro' || selectedPageId.value === 'm2-review-manage' || selectedPageId.value.startsWith('api-') || selectedPageId.value.startsWith('flow-'));
const isModule3Page = computed(() => selectedPageId.value === 'm3-intro' || selectedPageId.value.startsWith('m3-'));
const isApiTreeActive = computed(() => selectedPageId.value.startsWith('api-'));
const isFlowTreeActive = computed(() => selectedPageId.value.startsWith('flow-'));
const currentApi = computed(() => apis.find((a) => a.pageId === selectedPageId.value) || null);
const currentFlow = computed(() => flowPages.value.find((f) => f.pageId === selectedPageId.value) || null);
const currentMiddleware = computed(() => middlewareMap[selectedPageId.value] || null);
const laneHoverStyle = computed(() => ({ left: `${laneHover.x}px`, top: `${laneHover.y}px` }));

function expandFor(id) {
  if (id === 'm1' || id === 'm1-config') module1Expanded.value = true;
  if (id === 'm2-intro' || id === 'm2-review-manage') module2Expanded.value = true;
  if (id.startsWith('api-')) { module2Expanded.value = true; apiTreeExpanded.value = true; const api = apis.find((a) => a.pageId === id); if (api) apiGroupExpanded[api.group] = true; }
  if (id.startsWith('flow-')) { module2Expanded.value = true; flowTreeExpanded.value = true; }
  if (id === 'm3-intro' || id.startsWith('m3-')) module3Expanded.value = true;
}
function selectPage(id) { selectedPageId.value = id; expandFor(id); }
function openApiPage(id) { selectPage(id); }
function openFlowPage(fid) { selectPage('flow-' + fid.toLowerCase()); }
function toggleApiGroup(key) { apiGroupExpanded[key] = !apiGroupExpanded[key]; }
function openPage(id) { if (!id) return; if (id.startsWith('/api/')) { const api = apiByPath[id]; if (api) openApiPage(api.pageId); } else selectPage(id); }
function eventSpanStyle(evt) {
  const start = Math.min(evt.from, evt.to) + 2;
  const end = Math.max(evt.from, evt.to) + 3;
  return { gridColumn: `${start} / ${end}` };
}
function setActiveLane(idx) { activeLane.value = idx; }
function clearActiveLane() {
  activeLane.value = -1;
  laneHover.visible = false;
}
function eventTouchesLane(evt, laneIdx) {
  if (laneIdx < 0) return false;
  const min = Math.min(evt.from, evt.to);
  const max = Math.max(evt.from, evt.to);
  return laneIdx >= min && laneIdx <= max;
}
function eventConnectorClass(evt) {
  if (evt.from === evt.to) return 'connector--center';
  return evt.from > evt.to ? 'connector--left' : 'connector--right';
}
function handleSeqMousemove(event) {
  const host = event.currentTarget;
  if (!host || typeof host.querySelectorAll !== 'function') return;
  const lanes = host.querySelectorAll('.stripe-head .stripe-lane');
  if (!lanes.length) return;
  const x = event.clientX;
  const hostRect = host.getBoundingClientRect();
  let matched = -1;
  lanes.forEach((lane, idx) => {
    const rect = lane.getBoundingClientRect();
    if (x >= rect.left && x <= rect.right) matched = idx;
  });
  activeLane.value = matched;
  laneHover.visible = matched >= 0;
  laneHover.text = matched >= 0 && currentFlow.value ? currentFlow.value.swimlanes[matched] : '';
  laneHover.x = Math.max(8, event.clientX - hostRect.left + 12);
  laneHover.y = Math.max(8, event.clientY - hostRect.top - 12);
}

const allPageIds = computed(() => ['m1', 'm1-config', 'm2-intro', 'm2-review-manage', 'm3-intro', 'm4', 'm5', ...apis.map((a) => a.pageId), ...flowPages.value.map((f) => f.pageId), ...middlewarePages.map((m) => m.pageId)]);
onMounted(() => {
  const hash = decodeURIComponent(window.location.hash.replace(/^#/, ''));
  if (hash && allPageIds.value.includes(hash)) selectPage(hash);
  nextTick(() => {
    syncRedisSequenceViewportWidth();
    annotateDocTerms();
    if (typeof ResizeObserver !== 'undefined' && docsMainRef.value) {
      docsMainResizeObserver = new ResizeObserver(() => syncRedisSequenceViewportWidth());
      docsMainResizeObserver.observe(docsMainRef.value);
    }
  });
  window.addEventListener('resize', syncRedisSequenceViewportWidth);
});
watch(selectedPageId, (id) => {
  const h = '#' + encodeURIComponent(id);
  if (window.location.hash !== h) window.history.replaceState(null, '', window.location.pathname + window.location.search + h);
  if (redisFlowDialog.visible) closeRedisFlowDialog();
  nextTick(() => {
    syncRedisSequenceViewportWidth();
    annotateDocTerms();
  });
});
onBeforeUnmount(() => {
  docsMainResizeObserver?.disconnect();
  docsMainResizeObserver = null;
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', syncRedisSequenceViewportWidth);
  }
  if (typeof document !== 'undefined') {
    document.body.classList.remove('redis-flow-modal-open');
  }
});
</script>
<style scoped lang="scss">
.docs-shell {
  --bg: #f6f6f7;
  --panel: #fff;
  --line: #e5e7eb;
  --text: #2c3e50;
  --sub: #5f6b7a;
  --brand: #42b883;
  --mono: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', Consolas, Menlo, monospace;
  --app-header-offset: 74px;
  min-height: 100%;
  background: var(--bg);
  color: var(--text);
  font-size: 15px;
  line-height: 1.7;
}

.docs-sidebar {
  position: fixed;
  left: 0;
  top: var(--app-header-offset);
  width: 318px;
  height: calc(100vh - var(--app-header-offset));
  overflow-y: auto;
  border-right: 1px solid var(--line);
  background: var(--panel);
  padding: 14px 10px 20px;
}

.brand {
  padding: 10px 10px 14px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 10px;
}

.brand .tag {
  margin: 0;
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
}

.brand h1 {
  margin: 6px 0;
  font-size: 27px;
  line-height: 1.25;
}

.brand p {
  margin: 0;
  color: var(--sub);
  font-size: 13px;
}

.tree,
.branch,
.lvl {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.lvl2 {
  padding-left: 10px;
}

.lvl4 {
  padding-left: 12px;
}

.lvl5 {
  padding-left: 14px;
}

.node {
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 8px;
  color: #3c4858;
  display: flex;
  gap: 8px;
  align-items: center;
}

.node:hover {
  background: #f0f2f5;
}

.node.active {
  background: #e8f8f0;
  color: #1f8f62;
  box-shadow: inset 2px 0 0 var(--brand);
}

.l1 {
  font-size: 15px;
  font-weight: 700;
}

.l2,
.l3 {
  font-size: 14px;
  font-weight: 600;
}

.l4,
.l5 {
  font-size: 13.5px;
}

.group {
  color: #5a6472;
}

.count {
  margin-left: auto;
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 999px;
  background: #eef2f7;
  color: #6b7280;
}

.chev {
  margin-left: auto;
  color: #8892a0;
}

.api-cn {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.docs-main {
  margin-left: 318px;
  padding: 22px 24px 34px;
}

.page {
  border: none;
  border-radius: 0;
  background: transparent;
  padding: 8px 0 0;
}

.page > header:not(.api-hero) {
  border-bottom: 1px solid #dbe3ee;
  padding-bottom: 12px;
}

.page h2 {
  margin: 0;
  font-size: 28px;
}

.page p {
  color: var(--sub);
  line-height: 1.75;
  font-size: 15px;
}

.path {
  font-family: var(--mono);
  color: #1f2937 !important;
  font-size: 13px;
}

.block {
  margin-top: 16px;
  border: none;
  border-top: 1px solid #e1e7ee;
  border-radius: 0;
  padding: 13px 0 0;
  background: transparent;
}

.block h3 {
  margin: 0 0 9px;
  font-size: 20px;
}

.block h4 {
  margin: 0 0 8px;
  font-size: 16px;
}

.table-wrap {
  overflow-x: auto;
  border: none;
  border-radius: 0;
}

table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
  font-size: 14px;
}

th,
td {
  border-bottom: 1px solid var(--line);
  padding: 11px;
  text-align: left;
  vertical-align: top;
}

th {
  background: #f8fafc;
  font-size: 13px;
}

.mono {
  font-family: var(--mono);
  font-size: 13px;
}

.info-lines {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-lines li {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 8px;
  padding: 9px 11px;
  border: 1px solid #e8edf3;
  background: #f8fafc;
  border-radius: 8px;
}

.info-lines span {
  color: #6b7280;
  font-size: 13px;
}

.info-lines b {
  font-family: var(--mono);
  color: #111827;
  font-size: 13px;
  font-weight: 500;
}

.code {
  border: 1px solid #1f2937;
  border-radius: 11px;
  background: linear-gradient(180deg, #0f172a, #111827);
  overflow: hidden;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.code pre {
  margin: 0;
  padding: 16px;
  overflow-x: auto;
  font-family: var(--mono) !important;
  font-size: 13px;
  line-height: 1.7;
  color: #f8fafc !important;
  background: transparent !important;
  text-shadow: none !important;
  -webkit-text-fill-color: #f8fafc !important;
}

.code code {
  font-family: var(--mono) !important;
  color: #f8fafc !important;
  background: transparent !important;
  -webkit-text-fill-color: #f8fafc !important;
}

.docs-shell :deep(pre),
.docs-shell :deep(code) {
  font-family: var(--mono) !important;
}

.docs-shell :deep(pre::before),
.docs-shell :deep(pre::after),
.docs-shell :deep(code::before),
.docs-shell :deep(code::after),
.code pre::before,
.code pre::after,
.code code::before,
.code code::after {
  content: none !important;
  display: none !important;
}

.docs-shell :deep(pre *),
.docs-shell :deep(code *) {
  background: transparent !important;
}

.chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 21px;
  border-radius: 999px;
  font-size: 12px;
  font-family: var(--mono);
  font-weight: 700;
  color: #fff;
  padding: 0 8px;
  flex: 0 0 auto;
}

.chip--get {
  background: #2563eb;
}

.chip--post {
  background: #059669;
}

.chip--put {
  background: #d97706;
}

.chip--delete {
  background: #dc2626;
}

.link {
  border: none;
  background: transparent;
  color: #0f766e;
  text-decoration: underline;
  cursor: pointer;
  padding: 0;
  font-size: 13px;
}

.link:hover {
  color: #0b5f58;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.jump {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.page.api-doc {
  padding-top: 0;
}

.api-doc {
  background: transparent;
}

.api-hero {
  border: none;
  border-bottom: 1px solid #dbe3ee;
  border-radius: 0;
  background: transparent;
  padding: 0 0 12px;
}

.api-hero__title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.api-hero__title h2 {
  margin: 0;
}

.api-meta {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.meta-pill {
  border: 1px solid #d8e2ec;
  background: #f8fafc;
  color: #334155;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  line-height: 1.4;
}

.link-btn {
  cursor: pointer;
}

.api-layout {
  margin-top: 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 440px;
  gap: 16px;
  align-items: start;
}

.api-main {
  min-width: 0;
}

.api-aside {
  min-width: 0;
}

.api-doc .api-card {
  border: none;
  border-radius: 0;
  background: transparent;
  padding: 0;
}

.api-doc .api-aside .api-card {
  border-left: 1px solid #dbe3ee;
  padding-left: 14px;
}

.api-doc .endpoint-line {
  border: none;
  background: #eef2f7;
  padding: 7px 10px;
}

.api-sticky {
  position: sticky;
  top: calc(var(--app-header-offset) + 12px);
}

.api-aside .code pre {
  white-space: pre-wrap;
  overflow-x: hidden;
  word-break: break-word;
}

.endpoint-line {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  background: #f8fafc;
  margin-bottom: 8px;
}

.endpoint-line code {
  font-family: var(--mono);
  color: #0f172a;
  font-size: 13px;
}

.param-table {
  min-width: 760px;
}

.stripe-seq {
  --seq-order-col: 34px;
  border: 1px solid #dbe3ee;
  border-radius: 12px;
  background: #fff;
  overflow: visible;
  padding: 10px;
  position: relative;
}

.lane-hover-tip {
  position: absolute;
  z-index: 8;
  border: 1px solid rgba(147, 197, 253, 0.9);
  background: rgba(239, 246, 255, 0.92);
  color: #1d4ed8;
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
  font-family: var(--mono);
  pointer-events: none;
  backdrop-filter: blur(2px);
  transform: translate(-50%, -120%);
}

.stripe-head {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  margin-left: calc(var(--seq-order-col) + 8px);
  width: calc(100% - var(--seq-order-col) - 8px);
  margin-bottom: 10px;
  position: sticky;
  top: calc(var(--app-header-offset) + 8px);
  z-index: 6;
  background: #fff;
  padding-bottom: 6px;
  border-bottom: 1px solid transparent;
}

.stripe-head.active {
  border-bottom-color: #dbe3ee;
  box-shadow: 0 6px 12px rgba(15, 23, 42, 0.06);
}

.stripe-lane {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #dbe3ee;
  background: #f8fafc;
  border-radius: 10px;
  padding: 7px 8px;
  font-size: 13px;
  color: #334155;
  transition: background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.stripe-lane.active {
  border-color: #60a5fa;
  background: #eff6ff;
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.25);
}

.lane-index {
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #0f172a;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--mono);
  font-size: 11px;
}

.stripe-body {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.flow-track {
  display: grid;
  grid-template-columns: var(--seq-order-col) 1fr;
  gap: 8px;
  align-items: center;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: #fbfdff;
  padding: 8px 10px;
  position: relative;
  z-index: 2;
}

.track-title {
  font-family: var(--mono);
  color: #64748b;
  font-size: 11px;
}

.track-chain {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.track-node {
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #334155;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 12px;
  cursor: pointer;
}

.track-node.active {
  border-color: #60a5fa;
  background: #eff6ff;
  color: #1d4ed8;
}

.track-arrow {
  color: #94a3b8;
  font-size: 12px;
  letter-spacing: 0.5px;
}

.lane-guides {
  position: absolute;
  top: 50px;
  left: calc(var(--seq-order-col) + 8px);
  right: 0;
  bottom: 0;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  pointer-events: none;
  z-index: 0;
}

.lane-guide {
  position: relative;
  border-radius: 10px;
  background: rgba(148, 163, 184, 0.02);
}

.lane-guide::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  border-left: 1px dashed rgba(148, 163, 184, 0.35);
}

.lane-guide.active {
  background: rgba(59, 130, 246, 0.08);
}

.lane-guide.active::before {
  border-left-color: rgba(37, 99, 235, 0.55);
}

.stripe-event {
  display: grid;
  grid-template-columns: var(--seq-order-col) repeat(6, minmax(0, 1fr));
  gap: 8px;
  align-items: start;
  position: relative;
  z-index: 2;
}

.event-order {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: 1px solid #bfd4ea;
  background: #eff6ff;
  color: #1d4ed8;
  font-family: var(--mono);
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 8px;
}

.event-line {
  height: 2px;
  background: linear-gradient(90deg, rgba(148, 163, 184, 0.75) 0%, rgba(96, 165, 250, 0.9) 82%);
  margin: 8px 10px 0;
  border-radius: 999px;
  position: relative;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.25) inset;
}

.event-line.reverse {
  background: linear-gradient(90deg, rgba(96, 165, 250, 0.9), rgba(148, 163, 184, 0.75));
}

.event-line.self {
  background: rgba(96, 165, 250, 0.95);
  width: 70%;
}

.event-line.active {
  height: 2px;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.95) 0%, rgba(37, 99, 235, 1) 100%);
  box-shadow: 0 0 10px rgba(59, 130, 246, 0.25);
}

.event-card {
  position: relative;
  border: 1px solid #d6e1ec;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  padding: 12px;
  margin-top: 14px;
  box-shadow: 0 8px 20px -18px rgba(30, 41, 59, 0.6), 0 1px 2px rgba(15, 23, 42, 0.05);
}

.event-stack {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.event-connector {
  --conn-color: rgba(96, 165, 250, 0.92);
  position: relative;
  width: 0;
  height: 14px;
  margin-top: -1px;
  border-left: 2px dashed var(--conn-color);
}

.event-connector.active {
  --conn-color: rgba(37, 99, 235, 1);
}

.event-connector::after {
  content: '';
  position: absolute;
  left: -5px;
  bottom: -6px;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 7px solid var(--conn-color);
}

.connector--right {
  align-self: flex-end;
  margin-right: 10px;
}

.connector--left {
  align-self: flex-start;
  margin-left: 10px;
}

.connector--center {
  align-self: center;
}

.event-card.active {
  border-color: #60a5fa;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.12), 0 10px 20px -16px rgba(37, 99, 235, 0.45);
  background: linear-gradient(180deg, #ffffff 0%, #f6faff 100%);
}

.event-card h4 {
  margin: 0;
}

.event-card p {
  margin: 8px 0 0;
  color: #475569;
  font-size: 14px;
}

.seq-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 9px;
}

.badge {
  border: 1px solid #a7f3d0;
  background: #ecfdf5;
  color: #047857;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 12px;
  line-height: 1.4;
}

.refs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 9px;
}

.ref {
  border: 1px solid #d1d5db;
  border-radius: 999px;
  background: #fff;
  color: #1f2937;
  cursor: pointer;
  padding: 4px 8px;
  display: inline-flex;
  gap: 6px;
  align-items: center;
  font-size: 13px;
}

.ref:hover {
  border-color: #42b883;
  background: #f6fffa;
}

.layer-flow {
  display: flex;
  align-items: stretch;
  gap: 10px;
  flex-wrap: wrap;
  padding-bottom: 4px;
}

.layer-card {
  flex: 0 0 260px;
  min-width: 260px;
  border: 1px solid #cfe6db;
  background: #f5fffa;
  border-radius: 12px;
  padding: 11px;
}

.layer-card p {
  margin: 8px 0 0;
  font-size: 14px;
}

.layer-arrow {
  align-self: center;
  color: #0ea5a0;
  font-size: 24px;
  font-weight: 700;
  flex: 0 0 auto;
}

.notes {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notes li {
  padding: 9px 11px;
  border: 1px solid #dbeafe;
  border-left: 4px solid #3b82f6;
  border-radius: 8px;
  background: #f8fbff;
  font-size: 14px;
  color: #334155;
}

.notes strong {
  margin-right: 6px;
  color: #1d4ed8;
  font-family: var(--mono);
  font-size: 12px;
}

:deep(.term-note) {
  color: #15803d !important;
  font-weight: 600;
  background: #ecfdf3;
  border-radius: 4px;
  padding: 0 3px;
  cursor: help;
}

:deep(.term-note:hover) {
  color: #166534 !important;
  background: #dcfce7;
}

.redis-hero-block {
  overflow: hidden;
}

.redis-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.95fr);
  gap: 18px;
  padding: 20px;
  border-radius: 20px;
  background:
    radial-gradient(circle at top left, rgba(56, 189, 248, 0.18), transparent 34%),
    radial-gradient(circle at right center, rgba(251, 191, 36, 0.2), transparent 30%),
    linear-gradient(135deg, #0f172a 0%, #0b253f 45%, #15304f 100%);
  color: #e2e8f0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.redis-kicker {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(15, 23, 42, 0.35);
  color: #93c5fd;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.redis-hero h3 {
  margin: 12px 0 10px;
  font-size: 28px;
  color: #f8fafc;
}

.redis-hero p {
  margin: 0;
  line-height: 1.75;
  color: rgba(226, 232, 240, 0.9);
}

.redis-hero-tags {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.redis-hero-tag {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid var(--tone-border, rgba(125, 211, 252, 0.35));
  background: var(--tone-bg, rgba(255, 255, 255, 0.06));
  color: var(--tone-ink, #e2e8f0);
  font-size: 12px;
  font-weight: 700;
  backdrop-filter: blur(10px);
}

.redis-hero-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.redis-stat-card {
  min-height: 124px;
  padding: 14px 14px 12px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.11), rgba(255, 255, 255, 0.04));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.redis-stat-value {
  font-size: 32px;
  font-weight: 800;
  color: #f8fafc;
  line-height: 1;
}

.redis-stat-label {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #bae6fd;
}

.redis-stat-card p {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(226, 232, 240, 0.78);
}

.redis-overview-grid,
.redis-strategy-grid,
.redis-key-groups {
  display: grid;
  gap: 14px;
}

.redis-overview-grid,
.redis-strategy-grid,
.redis-key-groups {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.redis-overview-card,
.redis-strategy-card,
.redis-key-group {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid var(--tone-border, #dbeafe);
  background: linear-gradient(180deg, #ffffff, var(--tone-bg, #f8fbff));
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.06);
}

.redis-overview-card::before,
.redis-strategy-card::before,
.redis-key-group::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 5px;
  background: var(--tone-solid, #38bdf8);
}

.redis-overview-card,
.redis-strategy-card,
.redis-key-group {
  padding: 16px 16px 14px 18px;
}

.redis-overview-head,
.redis-key-group-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.redis-overview-head h4,
.redis-key-group-head h4 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.redis-overview-badge,
.redis-key-group-count {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--tone-chip-bg, #e0f2fe);
  color: var(--tone-ink, #0f172a);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.redis-overview-card p,
.redis-key-group-head p,
.redis-strategy-card p {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.7;
}

.redis-overview-meta {
  margin-top: 14px;
  display: grid;
  gap: 7px;
  font-size: 12px;
  color: #334155;
}

.redis-overview-meta strong {
  color: #0f172a;
}

.redis-strategy-title {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.redis-strategy-kv {
  margin-top: 12px;
  display: grid;
  gap: 4px;
}

.redis-strategy-kv span {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.redis-strategy-kv strong,
.redis-strategy-kv code {
  color: #0f172a;
  font-size: 13px;
}

.redis-key-group-list {
  margin-top: 14px;
  display: grid;
  gap: 10px;
}

.redis-key-card {
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(255, 255, 255, 0.82);
  padding: 12px;
}

.redis-key-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.redis-key-card-head code {
  font-size: 12px;
  line-height: 1.6;
  color: #0f172a;
  word-break: break-all;
}

.redis-key-card-ttl {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  background: #f8fafc;
  color: #334155;
  font-size: 11px;
  font-weight: 700;
  border: 1px solid #e2e8f0;
}

.redis-key-card p {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.65;
  color: #334155;
}

.redis-key-card-meta {
  margin-top: 10px;
  display: grid;
  gap: 4px;
  font-size: 11px;
  color: #64748b;
}

.redis-archive-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.redis-archive-head h3 {
  margin: 0;
}

.redis-archive-head p {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.7;
}

.redis-archive-toggle {
  border: 1px solid #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.redis-archive-toggle:hover {
  background: #dbeafe;
}

.redis-archive-toolbar {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.redis-archive-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.redis-archive-pill {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #dbe5ee;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.redis-archive-search {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.redis-archive-search input {
  width: min(320px, 72vw);
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 9px 12px;
  background: #fff;
  color: #0f172a;
  font-size: 13px;
}

.redis-archive-search input:focus {
  outline: none;
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.18);
}

.redis-archive-groups {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.redis-archive-chip {
  border: 1px solid #dbe5ee;
  background: #fff;
  color: #334155;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.redis-archive-chip.active,
.redis-archive-chip:hover {
  border-color: #60a5fa;
  background: #eff6ff;
  color: #1d4ed8;
}

.redis-archive-table {
  margin-top: 14px;
}

.redis-archive-preview {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.redis-archive-preview-card {
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  padding: 12px;
}

.redis-archive-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.redis-archive-preview-group {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 700;
}

.redis-archive-preview-ttl {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.redis-archive-preview-card code {
  display: block;
  margin-top: 10px;
  color: #0f172a;
  font-size: 12px;
  line-height: 1.6;
  word-break: break-all;
}

.redis-archive-preview-card p {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.65;
  font-size: 13px;
}

.tone-teal {
  --tone-bg: #ecfeff;
  --tone-border: #99f6e4;
  --tone-solid: #14b8a6;
  --tone-ink: #0f766e;
  --tone-chip-bg: #ccfbf1;
}

.tone-rose {
  --tone-bg: #fff1f2;
  --tone-border: #fecdd3;
  --tone-solid: #f43f5e;
  --tone-ink: #be123c;
  --tone-chip-bg: #ffe4e6;
}

.tone-sky {
  --tone-bg: #eff6ff;
  --tone-border: #bfdbfe;
  --tone-solid: #3b82f6;
  --tone-ink: #1d4ed8;
  --tone-chip-bg: #dbeafe;
}

.tone-amber {
  --tone-bg: #fff7ed;
  --tone-border: #fdba74;
  --tone-solid: #f59e0b;
  --tone-ink: #c2410c;
  --tone-chip-bg: #ffedd5;
}

.tone-cyan {
  --tone-bg: #ecfeff;
  --tone-border: #a5f3fc;
  --tone-solid: #06b6d4;
  --tone-ink: #0e7490;
  --tone-chip-bg: #cffafe;
}

.tone-emerald {
  --tone-bg: #ecfdf5;
  --tone-border: #a7f3d0;
  --tone-solid: #10b981;
  --tone-ink: #047857;
  --tone-chip-bg: #d1fae5;
}

.tone-violet {
  --tone-bg: #f5f3ff;
  --tone-border: #ddd6fe;
  --tone-solid: #8b5cf6;
  --tone-ink: #6d28d9;
  --tone-chip-bg: #ede9fe;
}

.tone-orange {
  --tone-bg: #fff7ed;
  --tone-border: #fed7aa;
  --tone-solid: #f97316;
  --tone-ink: #c2410c;
  --tone-chip-bg: #ffedd5;
}

.tone-indigo {
  --tone-bg: #eef2ff;
  --tone-border: #c7d2fe;
  --tone-solid: #6366f1;
  --tone-ink: #4338ca;
  --tone-chip-bg: #e0e7ff;
}

.redis-flow {
  border: 1px solid #dbe5ee;
  border-radius: 12px;
  padding: 12px;
  background: #fff;
  margin-top: 12px;
}

.redis-flow h4 {
  margin: 0 0 8px;
  font-size: 16px;
}

.redis-flow-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.redis-flow-title-text {
  display: inline-flex;
  align-items: center;
}

.flow-open-btn {
  border: 1px solid #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
  line-height: 1.3;
  white-space: nowrap;
}

.flow-open-btn:hover {
  background: #dbeafe;
}

.redis-flow-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
  padding: 16px;
  overflow: hidden;
}

.redis-flow-modal {
  width: min(960px, 96vw);
  max-height: 88vh;
  overflow: hidden;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #cbd5e1;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.25);
  display: flex;
  flex-direction: column;
  isolation: isolate;
}

.redis-flow-modal-head {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.redis-flow-modal-head h4 {
  margin: 0;
  font-size: 16px;
}

.flow-close-btn {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  border-radius: 8px;
  padding: 4px 10px;
  font-size: 12px;
  cursor: pointer;
}

.flow-close-btn:hover {
  background: #f1f5f9;
}

.redis-flow-modal-body {
  padding: 12px 14px 16px;
  overflow: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  overscroll-behavior: contain;
}

.redis-flow-modal-body::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.redis-flow-modal-lanes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.redis-flow-lane-chip {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
}

.redis-flowchart {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0 8px;
}

.redis-fc-node {
  width: min(760px, 92%);
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #fff;
  padding: 10px 12px;
  font-size: 15px;
  line-height: 1.7;
}

.redis-fc-node.start,
.redis-fc-node.end {
  width: min(260px, 72%);
  border-color: #86efac;
  background: #f0fdf4;
  text-align: center;
  font-weight: 700;
}

.redis-fc-node.merge {
  width: min(200px, 56%);
  border-color: #7dd3fc;
  background: #f0f9ff;
  text-align: center;
  font-weight: 700;
}

.redis-fc-node.process.branch {
  width: 100%;
}

.redis-fc-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.redis-fc-detail {
  margin-top: 4px;
  font-size: 15px;
  color: #334155;
  line-height: 1.7;
}

.redis-fc-arrow {
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0369a1;
  font-size: 18px;
}

.redis-fc-decision-note {
  width: min(760px, 92%);
  display: grid;
  gap: 2px;
  margin: -4px 0 8px;
  font-size: 15px;
  line-height: 1.7;
  color: #334155;
}

.redis-fc-decision-note span:first-child {
  font-weight: 700;
  color: #1e293b;
}

.redis-fc-decision-wrap {
  width: min(760px, 92%);
  display: flex;
  justify-content: center;
  padding: 8px 0 8px;
}

.redis-fc-node.decision {
  width: 136px;
  height: 136px;
  padding: 0;
  border-color: #f59e0b;
  background: #fffbeb;
  transform: rotate(45deg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.redis-fc-node.decision > span {
  transform: rotate(-45deg);
  text-align: center;
  width: 84%;
  font-size: 15px;
  color: #92400e;
  font-weight: 700;
  line-height: 1.45;
}

.redis-fc-branches {
  width: min(860px, 96%);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  align-items: start;
  margin-top: 2px;
  position: relative;
  padding-top: 18px;
}

.redis-fc-branches::before {
  content: '';
  position: absolute;
  left: 25%;
  right: 25%;
  top: 4px;
  border-top: 2px solid #94a3b8;
}

.redis-fc-branches::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  top: -14px;
  height: 18px;
  border-left: 2px solid #94a3b8;
}

.redis-fc-branch {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
}

.redis-fc-branch::before {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  top: -14px;
  height: 18px;
  border-left: 2px solid #94a3b8;
}

.redis-fc-branch::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  bottom: -18px;
  height: 18px;
  border-left: 2px solid #94a3b8;
}

.redis-fc-branch.yes {
  border-color: #5eead4;
  background: #f0fdfa;
}

.redis-fc-branch.no {
  border-color: #fdba74;
  background: #fff7ed;
}

.redis-fc-branch-head {
  display: inline-flex;
  align-self: flex-start;
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 15px;
  font-weight: 700;
}

.redis-fc-branch.yes .redis-fc-branch-head {
  border: 1px solid #2dd4bf;
  color: #0f766e;
  background: #ccfbf1;
}

.redis-fc-branch.no .redis-fc-branch-head {
  border: 1px solid #fb923c;
  color: #c2410c;
  background: #ffedd5;
}

.redis-fc-merge-wrap {
  width: min(760px, 92%);
  display: flex;
  justify-content: center;
  margin-top: 16px;
  position: relative;
  padding-top: 20px;
}

.redis-fc-merge-wrap::before {
  content: '';
  position: absolute;
  left: 25%;
  right: 25%;
  top: 0;
  border-top: 2px solid #94a3b8;
}

.redis-fc-merge-wrap::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  top: 0;
  height: 20px;
  border-left: 2px solid #94a3b8;
}

.redis-fc-merge-wrap .redis-fc-node.merge {
  position: relative;
  z-index: 1;
}

.redis-flow p {
  margin: 0 0 8px;
}

.redis-flow ul {
  margin: 0 0 10px;
  padding-left: 20px;
}

.redis-flow-modal-summary {
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #d7e4ef;
  border-radius: 18px;
  background:
    radial-gradient(circle at right top, rgba(59, 130, 246, 0.14), transparent 34%),
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.redis-flow-modal-summary p {
  margin: 0 0 8px;
}

.redis-flow-modal-summary ul {
  margin: 0;
  padding-left: 18px;
}

.redis-sequence {
  margin-top: 12px;
  border: 1px solid #d9e5ef;
  border-radius: 24px;
  padding: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(243, 248, 252, 0.96) 100%),
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.12), transparent 32%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.92),
    0 16px 34px rgba(15, 23, 42, 0.05);
}

.redis-sequence--modal {
  margin-top: 0;
}

.redis-sequence-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.redis-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #334155;
  border: 1px solid #dbe5ee;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  padding: 5px 10px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.04);
}

.redis-legend-item i {
  width: 18px;
  height: 3px;
  display: inline-block;
  border-radius: 99px;
}

.redis-legend-item i.main {
  background: #2563eb;
}

.redis-legend-item i.reply {
  background: repeating-linear-gradient(90deg, #64748b 0 7px, transparent 7px 11px);
}

.redis-legend-item i.alt {
  background: #10b981;
}

.redis-legend-item i.else {
  background: #f97316;
}

.redis-sequence-scroll {
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
}

.redis-sequence-canvas {
  display: block;
  width: auto;
  min-width: 0;
  max-width: none;
  height: auto;
}

.redis-sequence-surface {
  fill: #fbfdff;
  stroke: #d9e5ef;
  stroke-width: 1.2;
}

.redis-sequence-lane-line {
  stroke: rgba(148, 163, 184, 0.7);
  stroke-width: 1.5;
  stroke-dasharray: 8 10;
}

.redis-sequence-lane-card {
  stroke-width: 1.2;
}

.redis-sequence-lane-badge {
  fill: rgba(255, 255, 255, 0.96);
  stroke: rgba(148, 163, 184, 0.28);
  stroke-width: 1;
}

.redis-sequence-lane-badge-text {
  font-size: 9px;
  font-weight: 700;
  fill: #334155;
}

.redis-sequence-lane-label {
  font-size: 15px;
  font-weight: 700;
}

.redis-sequence-card-link {
  stroke-width: 1.2;
  stroke-dasharray: 4 4;
}

.redis-sequence-message-line {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.redis-sequence-card,
.redis-sequence-branch-box {
  stroke-width: 1.4;
}

.redis-sequence-step-pill {
  stroke: rgba(15, 23, 42, 0.05);
  stroke-width: 1;
}

.redis-sequence-step-text {
  font-size: 9px;
  font-weight: 700;
}

.redis-sequence-route-text {
  font-size: 15px;
  font-weight: 700;
}

.redis-sequence-action-text {
  font-size: 15px;
  font-weight: 700;
}

.redis-sequence-hint-text,
.redis-sequence-branch-tip-text {
  font-size: 15px;
}

.redis-sequence-tag-pill {
  stroke: rgba(15, 23, 42, 0.05);
  stroke-width: 1;
}

.redis-sequence-tag-text {
  font-size: 15px;
  font-weight: 700;
}

.redis-sequence-branch-title-text {
  font-size: 15px;
  font-weight: 700;
}

.tone-main {
  --tone-bg: #eff6ff;
  --tone-border: #bfdbfe;
  --tone-solid: #2563eb;
  --tone-ink: #1d4ed8;
  --tone-chip-bg: #dbeafe;
}

.tone-reply {
  --tone-bg: #f8fafc;
  --tone-border: #cbd5e1;
  --tone-solid: #64748b;
  --tone-ink: #334155;
  --tone-chip-bg: #e2e8f0;
}

.tone-alt {
  --tone-bg: #ecfdf5;
  --tone-border: #86efac;
  --tone-solid: #10b981;
  --tone-ink: #047857;
  --tone-chip-bg: #d1fae5;
}

.tone-else {
  --tone-bg: #fff7ed;
  --tone-border: #fdba74;
  --tone-solid: #f97316;
  --tone-ink: #c2410c;
  --tone-chip-bg: #ffedd5;
}

@media (max-width: 1024px) {
  .docs-sidebar {
    position: static;
    width: auto;
    height: auto;
    max-height: 50vh;
    border-right: none;
    border-bottom: 1px solid var(--line);
  }

  .docs-main {
    margin-left: 0;
  }

  .api-layout {
    grid-template-columns: 1fr;
  }

  .api-sticky {
    position: static;
    top: auto;
  }

  .api-doc .api-aside .api-card {
    border-left: none;
    border-top: 1px solid #dbe3ee;
    padding-left: 0;
    padding-top: 12px;
  }

  .redis-hero {
    grid-template-columns: 1fr;
  }

  .redis-overview-grid,
  .redis-strategy-grid,
  .redis-key-groups {
    grid-template-columns: 1fr;
  }

}

@media (max-width: 768px) {
  .docs-main {
    padding: 14px;
  }

  .page {
    padding: 6px 0 0;
  }

  .page h2 {
    font-size: 24px;
  }

  .stripe-seq {
    --seq-order-col: 30px;
  }

  .stripe-head,
  .stripe-event,
  .lane-guides {
    gap: 6px;
  }

  .event-order {
    width: 24px;
    height: 24px;
    margin-top: 6px;
  }

  .layer-card {
    min-width: 220px;
    flex-basis: 220px;
  }

  .redis-hero {
    padding: 16px;
    gap: 14px;
  }

  .redis-hero h3 {
    font-size: 24px;
  }

  .redis-hero-stats {
    grid-template-columns: 1fr;
  }

  .redis-overview-head,
  .redis-key-group-head,
  .redis-key-card-head {
    flex-direction: column;
    align-items: stretch;
  }

  .redis-archive-preview {
    grid-template-columns: 1fr;
  }

  .redis-sequence {
    padding: 12px;
    border-radius: 20px;
  }

  .redis-sequence-legend {
    gap: 8px;
  }

  .redis-sequence-canvas {
    min-width: 0;
  }

  .redis-fc-node.decision {
    width: 116px;
    height: 116px;
  }

  .redis-fc-branches {
    grid-template-columns: 1fr;
    padding-top: 12px;
  }

  .redis-fc-branches::before {
    left: 50%;
    right: auto;
    width: 0;
    border-top: 0;
    border-left: 2px solid #94a3b8;
    height: 12px;
    transform: translateX(-50%);
  }

  .redis-fc-branches::after {
    display: none;
  }

  .redis-fc-merge-wrap::before {
    left: 50%;
    right: auto;
    width: 0;
    border-top: 0;
    border-left: 2px solid #94a3b8;
    height: 20px;
    transform: translateX(-50%);
  }

}
:global(body.redis-flow-modal-open) {
  overflow: hidden;
}
</style>
