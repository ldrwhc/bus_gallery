// vite.config.js
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');

    return {
        plugins: [vue()],
        resolve: {
            alias: {
                '@': fileURLToPath(new URL('./src', import.meta.url))
            },
            // 补充：指定依赖扩展名，避免Rollup解析element-plus时的路径问题
            extensions: ['.mjs', '.js', '.jsx', '.json', '.vue', '.scss']
        },
        server: {
            port: Number(env.VITE_DEV_SERVER_PORT) || 5173,
            open: true,
            proxy: {
                '/api': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    secure: false,
                }
            }
        },
        css: {
            preprocessorOptions: {
                scss: {
                    additionalData: `@import "@/assets/styles/variables.scss";`
                    // 可选：若需要全局导入global.scss，替换为这一行（二选一，避免重复导入）
                    // additionalData: `@import "@/styles/global.scss";`
                }
            }
        },
        // 补充：构建配置，适配Docker部署的dist目录（可选但推荐）
        build: {
            outDir: 'dist', // vite默认输出dist，和nginx部署路径匹配
            assetsDir: 'assets',
            // 禁用CSS代码分割，避免element-plus样式加载问题
            cssCodeSplit: false,
            // 构建时清空dist目录
            emptyOutDir: true
        }
    };
});