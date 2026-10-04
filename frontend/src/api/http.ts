import axios from 'axios';

export const http = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});
