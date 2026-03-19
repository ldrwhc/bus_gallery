<template>
    <div class="page about-view">
        <main class="about-main constrained">
            <section class="hero">
                <div>
                    <p class="eyebrow">About</p>
                    <h1>Bus Gallery 技术栈与 QA</h1>
                    <p class="subtitle">
                        保留原有分组内容，改为分组按行横向展开，并补充代码、图表、流程图辅助说明。
                    </p>
                </div>
            </section>

            <section class="content-rows">
                <article class="card">
                    <div class="card__main">
                        <h2>前端技术栈</h2>
                        <ul>
                            <li>Vue 3 + Vite + Vue Router，按页面模块化组织视图</li>
                            <li>Vuex 做全局状态管理（车辆详情、用户会话、评论缓存等）</li>
                            <li>Axios 统一封装请求、鉴权头、异常处理与重试边界</li>
                            <li>Element Plus + SCSS 负责表单、弹窗和后台管理 UI</li>
                        </ul>
                        <div class="qa-item">
                            <p class="qa__q"><strong>Q：</strong>为什么这里还在用 Vuex？</p>
                            <p class="qa__a"><strong>A：</strong>现有状态模块已经围绕 Vuex 形成稳定结构，优先保证维护一致性和迭代速度。</p>
                        </div>
                        <div class="qa-item">
                            <p class="qa__q"><strong>Q：</strong>前端怎么处理重复车牌的详情切换？</p>
                            <p class="qa__a"><strong>A：</strong>详情弹窗支持 variants 聚合与左右切换，并通过初始图片定位确保“点哪张开哪张”。</p>
                        </div>
                    </div>
                    <aside class="card__side">
                        <h3>代码示意</h3>
                        <pre><code>// 重复车牌详情聚合
const snapshot = await fetchSnapshotByPlate(plate);
detail.variants = snapshot.variants;

// 初始图片定位
currentIndex.value = images.findIndex(
  (img) => String(img.id) === String(initialImageId)
);</code></pre>
                    </aside>
                </article>

                <article class="card">
                    <div class="card__main">
                        <h2>后端与数据层</h2>
                        <ul>
                            <li>Spring Boot 3.2 + Java 17 提供 REST API</li>
                            <li>MyBatis + Spring Data JPA 混合使用：查询与实体管理分工明确</li>
                            <li>MySQL 保存车辆、配置、审核单、评论、收藏等结构化数据</li>
                            <li>MinIO 存储原图与缩略图，Redis 负责会话与缓存</li>
                        </ul>
                        <div class="qa-item">
                            <p class="qa__q"><strong>Q：</strong>为什么同时用了 MyBatis 和 JPA？</p>
                            <p class="qa__a"><strong>A：</strong>复杂查询走 MyBatis 更可控，审核/用户等实体流程用 JPA 开发效率更高，按场景取长补短。</p>
                        </div>
                        <div class="qa-item">
                            <p class="qa__q"><strong>Q：</strong>对象存储为什么不用数据库 BLOB？</p>
                            <p class="qa__a"><strong>A：</strong>图片走 MinIO 能显著减轻数据库压力，便于静态资源访问和后续扩容。</p>
                        </div>
                    </div>
                    <aside class="card__side">
                        <h3>存储职责图</h3>
                        <div class="bars">
                            <div class="bar-row"><span>MySQL</span><div class="bar"><i style="width: 88%"></i></div></div>
                            <div class="bar-row"><span>Redis</span><div class="bar"><i style="width: 62%"></i></div></div>
                            <div class="bar-row"><span>MinIO</span><div class="bar"><i style="width: 96%"></i></div></div>
                        </div>
                        <p class="caption">分别承担：结构化数据 / 高速缓存 / 对象存储。</p>
                    </aside>
                </article>

                <article class="card">
                    <div class="card__main">
                        <h2>权限与审核链路</h2>
                        <ul>
                            <li>角色分为 STATION / REVIEWER / USER，后端强制 RBAC 校验</li>
                            <li>普通用户上传和改档提交审核，审核员按省级区域处理</li>
                            <li>站长可分配审核员角色和审核区域，含后台管理入口</li>
                            <li>审核支持通过/拒绝（含拒绝原因）并写回消息中心</li>
                        </ul>
                        <div class="qa-item">
                            <p class="qa__q"><strong>Q：</strong>如何保证审核员不能跨区操作？</p>
                            <p class="qa__a"><strong>A：</strong>后端在提交、审批、按 ID 修改等接口都做区域归属二次校验，不依赖前端限制。</p>
                        </div>
                        <div class="qa-item">
                            <p class="qa__q"><strong>Q：</strong>普通用户为什么不能直接入库？</p>
                            <p class="qa__a"><strong>A：</strong>通过审核单机制把写入分成“提交”和“审核通过入库”，减少错误数据污染主库。</p>
                        </div>
                    </div>
                    <aside class="card__side">
                        <h3>流程图</h3>
                        <div class="flow">
                            <span>用户提交</span>
                            <em>-></em>
                            <span>生成审核单</span>
                            <em>-></em>
                            <span>按省分配审核</span>
                            <em>-></em>
                            <span>通过入库/拒绝回执</span>
                        </div>
                    </aside>
                </article>

                <article class="card">
                    <div class="card__main">
                        <h2>安全与风控</h2>
                        <ul>
                            <li>登录、注册、找回、邮箱验证码均有 Redis 维度限流</li>
                            <li>触发阈值后启用图形验证码，阻断撞库与脚本攻击</li>
                            <li>邮箱验证码支持挑战码流程与过期校验（Spring Mail）</li>
                            <li>上传链路包含幂等键、队列上限与权限拦截</li>
                        </ul>
                        <div class="qa-item">
                            <p class="qa__q"><strong>Q：</strong>限流为什么不只放在网关？</p>
                            <p class="qa__a"><strong>A：</strong>网关做 IP 层兜底，应用层还能按账号/邮箱/业务场景做精细限流，粒度更细。</p>
                        </div>
                        <div class="qa-item">
                            <p class="qa__q"><strong>Q：</strong>密码相关接口如何做身份确认？</p>
                            <p class="qa__a"><strong>A：</strong>当前密码 + 邮箱验证码双因子校验，且验证码挑战与会话绑定，避免越权重置。</p>
                        </div>
                    </div>
                    <aside class="card__side">
                        <h3>限流策略图</h3>
                        <div class="bars">
                            <div class="bar-row"><span>登录</span><div class="bar"><i style="width: 84%"></i></div></div>
                            <div class="bar-row"><span>验证码</span><div class="bar"><i style="width: 52%"></i></div></div>
                            <div class="bar-row"><span>找回密码</span><div class="bar"><i style="width: 64%"></i></div></div>
                            <div class="bar-row"><span>上传提交</span><div class="bar"><i style="width: 76%"></i></div></div>
                        </div>
                    </aside>
                </article>

                <article class="card">
                    <div class="card__main">
                        <h2>性能与部署</h2>
                        <ul>
                            <li>车辆列表采用缓存与版本失效策略，降低重复查询成本</li>
                            <li>车牌快照接口聚合同牌多车数据，详情页首屏更快</li>
                            <li>Docker Compose 编排 MySQL / Redis / MinIO / Nginx / Backend</li>
                            <li>支持本地与服务器双环境配置（.env + .env.server）</li>
                        </ul>
                        <div class="qa-item">
                            <p class="qa__q"><strong>Q：</strong>线上故障时先看哪里？</p>
                            <p class="qa__a"><strong>A：</strong>先看 Nginx 与应用日志链路，再看 Redis/MinIO/MySQL 健康状态，最后排查环境变量配置。</p>
                        </div>
                        <div class="qa-item">
                            <p class="qa__q"><strong>Q：</strong>为什么强调双环境配置？</p>
                            <p class="qa__a"><strong>A：</strong>把本地调试与服务器参数解耦，减少误配引发的“本地好用、线上报错”。</p>
                        </div>
                    </div>
                    <aside class="card__side">
                        <h3>部署链路</h3>
                        <pre><code>Browser -> Nginx -> Spring Boot
                    -> MySQL
                    -> Redis
                    -> MinIO

compose:
  frontend / backend / mysql / redis / minio / nginx</code></pre>
                    </aside>
                </article>
            </section>
        </main>
    </div>
</template>

<style scoped lang="scss">
.page {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f5f7fb;
}

.constrained {
    width: min(1240px, 100%);
    margin: 0 auto;
    flex: 1;
    padding: 30px 20px 86px;
    box-sizing: border-box;
}

.hero {
    background: #0f172a;
    color: #fff;
    border-radius: 24px;
    padding: 30px;
    margin-bottom: 22px;
}

.eyebrow {
    letter-spacing: 0.3em;
    font-size: 0.8rem;
    text-transform: uppercase;
    opacity: 0.8;
    margin-bottom: 8px;
}

.subtitle {
    color: rgba(255, 255, 255, 0.75);
    margin-top: 8px;
}

.content-rows {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.card {
    background: #fff;
    border-radius: 18px;
    padding: 18px;
    box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
    display: grid;
    grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
    gap: 16px;
    align-items: start;
    min-width: 0;
    overflow: hidden;
}

.card__main {
    display: flex;
    flex-direction: column;
    gap: 10px;
    min-width: 0;
}

.card__main ul {
    padding-left: 16px;
    margin: 0;
    color: #475569;
    overflow-wrap: anywhere;
}

.card__main h2,
.card__main li,
.subtitle {
    overflow-wrap: anywhere;
    word-break: break-word;
}

.card__side {
    border-radius: 12px;
    padding: 12px;
    border: 1px solid #e2e8f0;
    background: #f8fafc;
    min-width: 0;
    overflow: hidden;
}

.card__side h3 {
    margin: 0 0 8px;
    color: #1e293b;
    font-size: 0.96rem;
}

.qa-item {
    border-radius: 12px;
    padding: 10px 12px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
}

.qa__q,
.qa__a {
    margin: 0;
    color: #475569;
    line-height: 1.5;
    overflow-wrap: anywhere;
}

.qa__q strong,
.qa__a strong {
    color: #1d4ed8;
}

pre {
    margin: 0;
    border-radius: 10px;
    background: #0f172a;
    color: #e2e8f0;
    padding: 12px;
    font-size: 0.8rem;
    line-height: 1.45;
    overflow-x: auto;
    max-width: 100%;
    box-sizing: border-box;
}

.flow {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
}

.flow span {
    padding: 6px 8px;
    border-radius: 8px;
    background: #e0e7ff;
    color: #3730a3;
    font-size: 0.82rem;
    font-weight: 600;
}

.flow em {
    color: #64748b;
    font-style: normal;
    font-weight: 700;
}

.bars {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.bar-row {
    display: grid;
    grid-template-columns: 72px 1fr;
    gap: 8px;
    align-items: center;
}

.bar-row span {
    color: #475569;
    font-size: 0.82rem;
}

.bar {
    height: 9px;
    border-radius: 999px;
    background: #dbe2ec;
    overflow: hidden;
}

.bar i {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, #2563eb, #0ea5e9);
}

.caption {
    margin: 8px 0 0;
    color: #64748b;
    font-size: 0.84rem;
}

@media (max-width: 980px) {
    .card {
        grid-template-columns: 1fr;
    }

    pre {
        white-space: pre-wrap;
        word-break: break-word;
    }
}
</style>
