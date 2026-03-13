const TOKEN_KEY = 'busGalleryToken';

export const persistToken = (token) => {
    if (token) {
        localStorage.setItem(TOKEN_KEY, token);
    } else {
        localStorage.removeItem(TOKEN_KEY);
    }
};

export const getToken = () => localStorage.getItem(TOKEN_KEY);

export { TOKEN_KEY };
