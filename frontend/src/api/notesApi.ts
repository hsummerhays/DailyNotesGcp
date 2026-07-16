import type { Note, CreateNoteRequest, UpdateNoteRequest, AuthResponse, ImportTaskStatus } from '../types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

// Auth is a JWT delivered as an httpOnly cookie by the backend - the browser attaches
// it automatically via `credentials: 'include'`. There is no token for JS to read or
// store, so nothing here touches localStorage.
const requestInit = (init: RequestInit = {}): RequestInit => ({
  ...init,
  credentials: 'include',
  headers: {
    'Content-Type': 'application/json',
    ...init.headers,
  },
});

export const authApi = {
  async register(email: string, password: string, displayName: string): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/register`, requestInit({
      method: 'POST',
      body: JSON.stringify({ email, password, displayName }),
    }));
    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || 'Registration failed');
    }
    return response.json();
  },

  async login(email: string, password: string): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, requestInit({
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }));
    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || 'Login failed');
    }
    return response.json();
  },

  async me(): Promise<AuthResponse | null> {
    const response = await fetch(`${API_BASE_URL}/auth/me`, requestInit());
    if (!response.ok) {
      return null;
    }
    return response.json();
  },

  async logout(): Promise<void> {
    await fetch(`${API_BASE_URL}/auth/logout`, requestInit({ method: 'POST' }));
  },
};

export const notesApi = {
  async getActiveNotes(query?: string): Promise<Note[]> {
    const url = new URL(`${API_BASE_URL}/notes`);
    if (query) {
      url.searchParams.append('query', query);
    }
    const response = await fetch(url.toString(), requestInit());
    if (!response.ok) {
      throw new Error('Failed to fetch active notes');
    }
    return response.json();
  },

  async getArchivedNotes(): Promise<Note[]> {
    const response = await fetch(`${API_BASE_URL}/notes/archived`, requestInit());
    if (!response.ok) {
      throw new Error('Failed to fetch archived notes');
    }
    return response.json();
  },

  async getNote(id: string): Promise<Note> {
    const response = await fetch(`${API_BASE_URL}/notes/${id}`, requestInit());
    if (!response.ok) {
      throw new Error(`Failed to fetch note with id ${id}`);
    }
    return response.json();
  },

  async createNote(request: CreateNoteRequest): Promise<Note> {
    const response = await fetch(`${API_BASE_URL}/notes`, requestInit({
      method: 'POST',
      body: JSON.stringify(request),
    }));
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || 'Failed to create note');
    }
    return response.json();
  },

  async updateNote(id: string, request: UpdateNoteRequest): Promise<Note> {
    const response = await fetch(`${API_BASE_URL}/notes/${id}`, requestInit({
      method: 'PUT',
      body: JSON.stringify(request),
    }));
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || 'Failed to update note');
    }
    return response.json();
  },

  async archiveNote(id: string): Promise<Note> {
    const response = await fetch(`${API_BASE_URL}/notes/${id}/archive`, requestInit({ method: 'PATCH' }));
    if (!response.ok) {
      throw new Error('Failed to archive note');
    }
    return response.json();
  },

  async restoreNote(id: string): Promise<Note> {
    const response = await fetch(`${API_BASE_URL}/notes/${id}/restore`, requestInit({ method: 'PATCH' }));
    if (!response.ok) {
      throw new Error('Failed to restore note');
    }
    return response.json();
  },

  async deleteNote(id: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/notes/${id}`, requestInit({ method: 'DELETE' }));
    if (!response.ok) {
      throw new Error('Failed to delete note');
    }
  },

  async triggerBulkImport(notes: CreateNoteRequest[]): Promise<ImportTaskStatus> {
    const response = await fetch(`${API_BASE_URL}/notes/import`, requestInit({
      method: 'POST',
      body: JSON.stringify({ notes }),
    }));
    if (!response.ok) {
      throw new Error('Failed to trigger bulk import');
    }
    return response.json();
  },

  async getImportStatus(taskId: string): Promise<ImportTaskStatus> {
    const response = await fetch(`${API_BASE_URL}/notes/import/${taskId}`, requestInit());
    if (!response.ok) {
      throw new Error('Failed to fetch import status');
    }
    return response.json();
  },
};
