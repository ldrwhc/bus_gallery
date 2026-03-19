<template>
    <div class="page about-view">
        <main class="about-main constrained">
            <section class="hero">
                <div>
                    <p class="eyebrow">About</p>
                    <h1>Bus Gallery 技术概览</h1>
                    <p class="subtitle">
                        这个页面主要介绍系统架构、关键技术方案与性能优化点，方便开发联调与面试讲解。
                    </p>
                </div>
            </section>

            <section class="content-grid">
                <article class="card">
                    <h2>前端技术栈</h2>
                    <p>
                        前端采用 Vue 3 + Vite，状态由 Vuex 管理，路由守卫控制登录访问，Axios 统一处理鉴权和异常。
                    </p>
                    <ul>
                        <li>业务按 Gallery / Upload / Catalog 分模块，减少耦合</li>
                        <li>接口层统一做 token 注入、401 兜底与消息提示</li>
                        <li>上传页采用 FormData + 超时控制，兼顾稳定性与体验</li>
                    </ul>
                    <div class="qa-item">
                        <p class="qa__q"><strong>Q：</strong>为什么用 Vuex 而不是 Pinia？</p>
                        <p class="qa__a"><strong>A：</strong>项目早期基于 Vuex 架构，优先保证团队维护成本和现有模块稳定性，避免迁移引入额外风险。</p>
                    </div>
                    <div class="qa-item">
                        <p class="qa__q"><strong>Q：</strong>路由鉴权怎么避免“假登录态”？</p>
                        <p class="qa__a"><strong>A：</strong>不仅看本地 token，还会在启动时拉取 profile 验证会话有效性，失效就清空状态并跳登录页。</p>
                    </div>
                </article>

                <article class="card">
                    <h2>后端与数据层</h2>
                    <p>
                        后端基于 Spring Boot，承担车辆档案、图片上传、评论收藏、快照聚合等核心能力。
                    </p>
                    <ul>
                        <li>MySQL 保存车辆、配置、评论、收藏等结构化数据</li>
                        <li>MinIO 存储原图和缩略图，降低数据库压力</li>
                        <li>Redis 同时承载会话、缓存和热点快照</li>
                    </ul>
                    <div class="qa-item">
                        <p class="qa__q"><strong>Q：</strong>上传流程里如何保证业务一致性？</p>
                        <p class="qa__a"><strong>A：</strong>上传接口放在事务边界内，车辆、配置、图片关联统一提交，任一环节异常都会回滚。</p>
                    </div>
                    <div class="qa-item">
                        <p class="qa__q"><strong>Q：</strong>图片处理失败时怎么避免脏数据？</p>
                        <p class="qa__a"><strong>A：</strong>失败会返回统一错误码，不保留半成品关联记录，避免出现“有图无档”或“有档无图”。</p>
                    </div>
                </article>

                <article class="card">
                    <h2>关键性能设计</h2>
                    <p>
                        性能设计聚焦“快读、抗抖动、一致性”，目标是高频访问下依然保持响应稳定。
                    </p>
                    <ul>
                        <li>车牌级快照（Redis big key）用于详情首屏直出</li>
                        <li>列表缓存采用版本号失效，写后自动淘汰旧缓存</li>
                        <li>上传接口支持幂等键，防止重复提交</li>
                        <li>游标分页替代 offset 深分页，减少大表扫描</li>
                    </ul>
                    <div class="qa-item">
                        <p class="qa__q"><strong>Q：</strong>为什么敢用 big key？</p>
                        <p class="qa__a"><strong>A：</strong>big key 只存“按车牌聚合后的可渲染快照”，读取收益高、结构稳定，且有 TTL 控制体积和时效。</p>
                    </div>
                    <div class="qa-item">
                        <p class="qa__q"><strong>Q：</strong>如何防止缓存脏读？</p>
                        <p class="qa__a"><strong>A：</strong>列表缓存绑定版本号，写操作递增版本，旧缓存自然失效；详情快照也通过版本与过期策略双保险。</p>
                    </div>
                </article>

                <article class="card">
                    <h2>工程化与部署</h2>
                    <p>项目支持本地开发与 Docker Compose 一键启动，适合联调、演示和面试现场说明。</p>
                    <ul>
                        <li>Nginx 统一代理前后端与对象存储访问路径</li>
                        <li>前后端配置按开发/生产环境隔离</li>
                        <li>接口文档与流程文档独立维护，降低协作成本</li>
                    </ul>
                    <div class="qa-item">
                        <p class="qa__q"><strong>Q：</strong>为什么选 Docker Compose？</p>
                        <p class="qa__a"><strong>A：</strong>能把 MySQL、Redis、MinIO、Nginx、后端服务一次拉起，环境复现成本低，联调效率高。</p>
                    </div>
                    <div class="qa-item">
                        <p class="qa__q"><strong>Q：</strong>线上和本地如何保持行为一致？</p>
                        <p class="qa__a"><strong>A：</strong>通过统一镜像、代理规则和环境变量约定，尽量让本地与部署环境共享同一套运行路径。</p>
                    </div>
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
    width: min(1200px, 100%);
    margin: 0 auto;
    flex: 1;
    padding: 32px 24px 96px;
    box-sizing: border-box;
}

.hero {
    background: #0f172a;
    color: #fff;
    border-radius: 28px;
    padding: 36px;
    margin-bottom: 32px;
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

.content-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 20px;
}

.card {
    background: #fff;
    border-radius: 20px;
    padding: 24px;
    box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
    display: flex;
    flex-direction: column;
    gap: 12px;

    ul,
    ol {
        padding-left: 16px;
        margin: 0;
        color: #475569;
    }
}

.qa-item {
    border-radius: 14px;
    padding: 10px 12px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
}

.qa__q,
.qa__a {
    margin: 0;
    color: #475569;
    line-height: 1.5;
}

.qa__q strong,
.qa__a strong {
    color: #1d4ed8;
}
</style>
