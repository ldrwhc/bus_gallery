module.exports = {
    root: true,
    env: {
        node: true
    },
    extends: [
        'plugin:vue/vue3-essential',
        'eslint:recommended',
        '@vue/prettier'
    ],
    parserOptions: {
        parser: '@babel/eslint-parser',
        requireConfigFile: false
    },
    globals: {
        defineProps: 'readonly',
        defineEmits: 'readonly',
        defineExpose: 'readonly',
        withDefaults: 'readonly'
    },
    rules: {
        'vue/multi-word-component-names': 'off'
    }
};