<template>
    <div class="page about-view">
        <main class="about-main constrained">
            <section class="hero">
                <div>
                    <p class="eyebrow">About</p>
                    <h1>关于 Bus Gallery</h1>
                    <p class="subtitle">
                        我们持续记录全国公交车辆的车牌、配置、上线时间与营运单位，帮助爱好者与从业者快速检索和比对信息。
                    </p>
                </div>
            </section>

            <section class="content-grid">
                <article class="card">
                    <h2>我们在做什么</h2>
                    <p>
                        Bus Gallery 汇聚来自各地的车辆照片、配置和上线资料，通过结构化的字段方便检索与统计。
                    </p>
                    <ul>
                        <li>可按地区 / 公司 / 品牌 / 车型分组浏览</li>
                        <li>时间轴呈现出厂、上线节点与车辆轨迹</li>
                        <li>配置字段覆盖动力、燃料、变速系统、踏步、悬挂等信息</li>
                    </ul>
                </article>

                <article class="card">
                    <h2>如何参与</h2>
                    <ol>
                        <li>注册账号并完成登录</li>
                        <li>在“上传”页面填写车辆信息并选择图片</li>
                        <li>关注审核进度，必要时补充说明</li>
                    </ol>
                    <p>系统会把已通过的上传归档到你的个人主页，方便后续管理与补充。</p>
                </article>

                <article class="card">
                    <h2>数据与存储</h2>
                    <p>
                        图片托管在 MinIO，对象元数据与车辆档案存放在 MySQL，Redis 用于缓存登录态与热门查询结果。
                    </p>
                    <p>欢迎通过 team@bus.gallery 与我们分享更多数据或合作想法。</p>
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
</style>
