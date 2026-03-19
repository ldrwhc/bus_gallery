import http from './axiosInstance';

export const fetchReviewInbox = () => http.get('/reviews/inbox');

export const fetchPendingSubmissions = () => http.get('/reviews/pending');

export const approveSubmission = (submissionId, payload) =>
    http.post(`/reviews/${submissionId}/approve`, { payload });

export const rejectSubmission = (submissionId, rejectCode, rejectReason) =>
    http.post(`/reviews/${submissionId}/reject`, { rejectCode, rejectReason });

export const submitVehicleUpdateReview = (vehicleId, imageId, payload) =>
    http.post('/reviews/submissions/update', { vehicleId, imageId, payload });
