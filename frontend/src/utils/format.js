import dayjs from 'dayjs';

export const formatDate = (value, fallback = '—') => {
    if (!value) return fallback;
    return dayjs(value).format('YYYY-MM-DD');
};