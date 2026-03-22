
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
      </nav>
    </aside>

    <main class="docs-main">
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
              <tr><td>REVIEWER</td><td>后端按审核地区限制</td><td>仅审核地区（按省级归属校验）</td><td>越权请求会被拒绝并返回未授权错误。</td></tr>
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
                <td>审核中心专用分页查询，支持 keyword、regionId、lastLaunch、lastId。</td>
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
                <td>提交编辑后的车辆信息，成功后刷新管理列表。</td>
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
            <li>审核员跨区修改失败：后端会按审核地区做写权限校验，非本审核范围车辆不可改删。</li>
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

        <section v-if="currentMiddleware.name === 'Redis'" class="block">
          <h3>Redis 面试重点</h3>
          <div class="table-wrap"><table><thead><tr><th>维度</th><th>实现细节</th></tr></thead><tbody>
            <tr><td>会话</td><td class="mono">busgallery:sessions:{token} / busgallery:user:sessions:{userId}</td></tr>
            <tr><td>限流</td><td class="mono">busgallery:rl:{scope}:{dimension}:{sha256(identity)}:{slot}</td></tr>
            <tr><td>验证码/风险</td><td class="mono">busgallery:auth:captcha:* / busgallery:auth:risk:*</td></tr>
            <tr><td>快照防击穿</td><td class="mono">bg:snapshot:plate:{plate}:latest|v{version}|stale|lock</td></tr>
            <tr><td>幂等</td><td class="mono">idempotent:upload:{Idempotency-Key}</td></tr>
          </tbody></table></div>
        </section>
      </article>

      <article v-else class="page">
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
    </main>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onMounted } from 'vue';

const rawApiLines = `
GET|/api/admin/overview
GET|/api/admin/submissions
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
    spring: 'Spring 在接收 multipart 后由 UploadSecurityService 校验 MIME、魔数、尺寸和像素，再调用 ImageService、VehicleService、SubmissionService 落业务数据并返回 APPROVED/PENDING。',
    db: '上传通过后 MySQL 会写入 image、vehicle、vehicle_image、vehicle_submission 关系数据并把主键回传给服务层。',
    other: '图片二进制会写入 MinIO 并返回 objectName/url，随后该元数据会入库供后续访问链路使用。'
  },
  {
    id: 'WF-06',
    title: '审核链路',
    frontend: '审核中心会把 submissionId、审批结果和驳回原因提交到 /api/reviews/inbox、/pending、/{id}/approve、/{id}/reject；车辆管理子页会调用 /api/vehicles/manage 与 /api/vehicles/{id}（查改删）维护车辆。',
    nginx: 'Nginx 把审核请求统一转发到 ReviewController 并附带代理头，保持入口一致。',
    redis: '审核通过或驳回后服务层会删除相关车辆页和快照缓存键，后续读取会回源最新数据。',
    spring: 'RoleGuard 先校验 REVIEWER/STATION 权限与区域范围，然后服务层在事务里变更 submission 与关联实体并返回新状态。',
    db: 'MySQL 更新 vehicle_submission 状态及关联主表记录后把结果返回给 Spring 形成审核响应。',
    other: '审核链路当前无 MQ，状态变更在同一事务中生效并立即反馈前端。'
  },
  {
    id: 'WF-07',
    title: '后台管理链路',
    frontend: '后台页面把字典维护、角色调整、车辆和图片管理操作转换成 CRUD 请求发送到 /api/admin/* 与相关写接口。',
    nginx: 'Nginx 对后台接口执行 /api 限流和反向代理后交给 AdminController 与对应业务 Controller。',
    redis: '后台写成功后会递增版本键或删除命中缓存，确保后续查询不再返回旧数据。',
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
    redis: 'SnapshotService 先查 latest/version 键，未命中再查 stale 并使用 lock 防击穿，命中直接返回，未命中才回源构建快照。',
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
  { pageId: 'm3-redis', name: 'Redis', intro: '会话、限流、验证码、幂等、快照、防击穿全部依赖 Redis。', role: '读链路做缓存，写链路做版本失效和幂等控制，是高频关键中间件。', io: [{ input: 'token/captcha/challenge/identity', process: '读写 key + TTL + 递增', output: '会话状态/限流判定' }, { input: 'plate 快照查询', process: 'latest/stale/lock 协同', output: '命中或回源快照' }, { input: 'Idempotency-Key', process: 'setIfAbsent 防重复', output: '允许或拒绝提交' }], configs: [{ key: 'spring.data.redis.*', file: 'backend/src/main/resources/application.yml', effect: '连接参数' }, { key: 'appendonly yes', file: 'docker/redis/redis.conf', effect: 'AOF 持久化' }, { key: 'auth.session.ttl-seconds', file: 'application.yml', effect: '会话 TTL' }, { key: 'busgallery.cache.*', file: 'application.yml', effect: '业务缓存 TTL' }], workflows: [{ flow: 'WF-03', input: '登录与验证码请求', process: 'session/captcha/risk key 管理', output: '认证状态' }, { flow: 'WF-05', input: '上传请求', process: '限流+幂等', output: '允许上传或拒绝' }, { flow: 'WF-09', input: '快照请求', process: '防击穿与缓存复用', output: '快照结果' }] },
  { pageId: 'm3-mysql', name: 'MySQL', intro: '承载用户、车辆、图片、评论、收藏、审核等核心持久化数据。', role: '通过 MyBatis/JPA 执行查询和事务写入。', io: [{ input: '业务请求参数', process: 'SQL 查询/更新', output: '实体与结果集' }, { input: '审核与后台写操作', process: '事务更新多表', output: '一致落库' }], configs: [{ key: 'spring.datasource.*', file: 'application.yml', effect: '连接地址和凭证' }, { key: 'spring.datasource.hikari.*', file: 'application.yml', effect: '连接池并发能力' }, { key: 'docker/init/init.sql', file: 'docker/init/init.sql', effect: '初始化表结构' }], workflows: [{ flow: 'WF-01', input: '车辆筛选条件', process: 'vehicle 相关表查询', output: '列表与详情' }, { flow: 'WF-06', input: '审核动作', process: 'submission 状态更新', output: '审核结果' }, { flow: 'WF-07', input: '后台 CRUD', process: '主数据维护', output: '最新主数据' }] },
  { pageId: 'm3-spring', name: 'Spring', intro: '鉴权拦截、控制器路由、服务层事务、异常处理均在 Spring 层。', role: 'WebMvcConfig 注入 AuthTokenInterceptor 到 /api/**，RoleGuard 做权限裁剪。', io: [{ input: 'HTTP 请求', process: 'Interceptor -> Controller -> Service -> Mapper', output: '统一响应' }, { input: 'Authorization/token', process: '会话解析并写入上下文', output: '用户上下文或 401' }], configs: [{ key: 'WebMvcConfig.addInterceptors', file: 'backend/config/WebMvcConfig.java', effect: '统一鉴权入口' }, { key: '@RequireLogin + RoleGuard', file: 'controller/auth', effect: '角色权限控制' }, { key: '@Transactional', file: 'service/controller', effect: '关键写链路事务一致性' }], workflows: [{ flow: 'WF-03', input: '认证请求', process: '认证/风控/会话签发', output: 'token' }, { flow: 'WF-05', input: '上传请求', process: '安全校验 + 幂等 + 业务写入', output: '上传结果' }, { flow: 'WF-10', input: '/api/metrics/db', process: '聚合慢 SQL 指标', output: '监控数据' }] },
  { pageId: 'm3-minio', name: 'MinIO', intro: '对象存储服务，负责图片数据落地与读取。', role: '上传写对象，访问通过后端签名校验后读取对象流。', io: [{ input: '上传文件', process: 'putObject 写入对象桶', output: 'objectName + url' }, { input: '签名 token', process: '验签后 getObject', output: '图片字节流' }], configs: [{ key: 'minio.endpoint/access-key/secret-key/bucket', file: 'application.yml + docker/.env', effect: '连接对象存储' }, { key: 'MINIO_CDN_HOST', file: 'docker/.env', effect: '外网访问域名' }, { key: 'location /bus-gallery/', file: 'docker/nginx/default.conf', effect: '统一对象访问入口' }], workflows: [{ flow: 'WF-05', input: '上传请求', process: '写对象并回写元数据', output: '可访问对象路径' }, { flow: 'WF-08', input: '图片访问请求', process: '验签后读取对象', output: '图片流' }] }
];
const middlewareMap = Object.fromEntries(middlewarePages.map((m) => [m.pageId, m]));
const middlewareEvents = [
  { trigger: '用户登录/注册', exec: 'Nginx -> Spring -> Redis -> MySQL', action: '限流、校验、写 session、返回 token', flow: 'WF-03', middlewarePages: ['m3-nginx', 'm3-spring', 'm3-redis', 'm3-mysql'] },
  { trigger: '请求车辆列表/详情', exec: 'Nginx -> Spring -> Redis -> MySQL -> MinIO', action: '缓存命中优先，未命中回源组装', flow: 'WF-01', middlewarePages: ['m3-nginx', 'm3-spring', 'm3-redis', 'm3-mysql', 'm3-minio'] },
  { trigger: '提交上传', exec: 'Nginx -> Spring -> Redis -> MinIO -> MySQL', action: '上传安全检查、幂等、防刷与落库', flow: 'WF-05', middlewarePages: ['m3-nginx', 'm3-spring', 'm3-redis', 'm3-minio', 'm3-mysql'] },
  { trigger: '审核操作', exec: 'Nginx -> Spring -> MySQL -> Redis', action: '角色校验、状态更新、缓存失效', flow: 'WF-06', middlewarePages: ['m3-nginx', 'm3-spring', 'm3-mysql', 'm3-redis'] },
  { trigger: '访问签名图片', exec: 'Nginx -> Spring -> MinIO', action: '验签与过期校验后返回对象流', flow: 'WF-08', middlewarePages: ['m3-nginx', 'm3-spring', 'm3-minio'] }
];

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
  if (g === 'Admin') return p.endsWith('/overview') ? '查询后台概览' : p.endsWith('/users') ? '查询后台用户' : p.includes('/users/{id}/role') ? '调整用户角色' : p.endsWith('/submissions') ? '查询提交流水' : m === 'GET' ? '查询字典数据' : m === 'POST' ? '新增字典数据' : m === 'PUT' ? '修改字典数据' : '删除字典数据';
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
function successExample(api) { if (api.group === 'Auth' && api.path.includes('/captcha')) return JSON.stringify({ captchaId: 'cap_xxx', imageBase64: 'data:image/png;base64,...', expireInSeconds: 180 }, null, 2); if (api.group === 'Auth' && api.method === 'POST' && (api.path.endsWith('/login') || api.path.endsWith('/register'))) return JSON.stringify({ token: 'session_xxx', profile: { id: 1, username: 'demo', role: 'USER' } }, null, 2); if (api.group === 'Vehicle' && api.method === 'GET' && api.path === '/api/vehicles') return JSON.stringify({ records: [{ vehicle: { id: 101, plateNumber: '粤B12345' } }], total: 1, page: 1, size: 12 }, null, 2); if (api.group === 'Image' && api.path.includes('/access/')) return 'HTTP/1.1 200 OK\nContent-Type: image/jpeg\n(binary stream)'; if (api.method === 'DELETE') return 'HTTP/1.1 200 OK\n(no response body)'; if (api.path.includes('/metrics/db')) return JSON.stringify({ total: 2331, slow: 13, samples: [{ sql: 'SELECT ...', ms: 211.2 }] }, null, 2); return JSON.stringify({ data: 'sample', success: true }, null, 2); }
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
  'WF-05': { frontend: ['multipart'], nginx: ['上传通道'], redis: ['幂等+限流'], spring: ['文件安全校验'], db: ['关系落库'], other: ['MinIO写入'] },
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
    { tag: '上传安全', title: '为什么做魔数校验', explain: '仅靠 MIME 容易伪造，魔数校验可以阻断伪装可执行文件。' }
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

const allPageIds = computed(() => ['m1', 'm1-config', 'm2-intro', 'm2-review-manage', 'm3-intro', 'm4', ...apis.map((a) => a.pageId), ...flowPages.value.map((f) => f.pageId), ...middlewarePages.map((m) => m.pageId)]);
onMounted(() => { const hash = decodeURIComponent(window.location.hash.replace(/^#/, '')); if (hash && allPageIds.value.includes(hash)) selectPage(hash); });
watch(selectedPageId, (id) => { const h = '#' + encodeURIComponent(id); if (window.location.hash !== h) window.history.replaceState(null, '', window.location.pathname + window.location.search + h); });
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
}
</style>
