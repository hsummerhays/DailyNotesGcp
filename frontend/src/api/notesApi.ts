import type { Note, CreateNoteRequest, UpdateNoteRequest } from '../types';

const API_BASE_URL = 'http://localhost:8080/api';

// Simple token storage helper for local dev
let authToken: string | null = localStorage.getItem('auth_token');

export const setAuthToken = (token: string | null) => {
  authToken = token;
  if (token) {
    localStorage.setItem('auth_token', token);
  } else {
    localStorage.removeItem('auth_token');
  }
};

export const getAuthToken = () => authToken;

const getHeaders = () => {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };
  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`;
  }
  return headers;
};

export const authApi = {
  async register(email: string, password: string, displayName: string): Promise<any> {
    const response = await fetch(`${API_BASE_URL}/auth/register`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ email, password, displayName }),
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || 'Registration failed');
    }
    const data = await response.json();
    setAuthToken(data.token);
    return data;
  },

  async login(email: string, password: string): Promise<any> {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ email, password }),
    });
    if (!response.ok) {
      const err = await response.json().catch(() => ({}));
      throw new Error(err.message || 'Login failed');
    }
    const data = await response.json();
    setAuthToken(data.token);
    return data;
  },

  logout() {
    setAuthToken(null);
  }
};

export const notesApi = {
  async getActiveNotes(query?: string): Promise<Note[]> {
    const url = new URL(`${API_BASE_URL}/notes`);
    if (query) {
      url.searchParams.append('query', query);
    }
    const response = await fetch(url.toString(), {
      headers: getHeaders(),
    });
    if (!response.ok) {
      throw new Error('Failed to fetch active notes');
    }
    return response.json();
  },

  async getArchivedNotes(): Promise<Note[]> {
    const response = await fetch(`${API_BASE_URL}/notes/archived`, {
      headers: getHeaders(),
    });
    if (!response.ok) {
      throw new Error('Failed to fetch archived notes');
    }
    return response.json();
  },

  async getNote(id: string): Promise<Note> {
    const response = await fetch(`${API_BASE_URL}/notes/${id}`, {
      headers: getHeaders(),
    });
    if (!response.ok) {
      throw new Error(`Failed to fetch note with id ${id}`);
    }
    return response.json();
  },

  async createNote(request: CreateNoteRequest): Promise<Note> {
    const response = await fetch(`${API_BASE_URL}/notes`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || 'Failed to create note');
    }
    return response.json();
  },

  async updateNote(id: string, request: UpdateNoteRequest): Promise<Note> {
    const response = await fetch(`${API_BASE_URL}/notes/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || 'Failed to update note');
    }
    return response.json();
  },

  async archiveNote(id: string): Promise<Note> {
    const response = await fetch(`${API_BASE_URL}/notes/${id}/archive`, {
      method: 'PATCH',
      headers: getHeaders(),
    });
    if (!response.ok) {
      throw new Error('Failed to archive note');
    }
    return response.json();
  },

  async restoreNote(id: string): Promise<Note> {
    const response = await fetch(`${API_BASE_URL}/notes/${id}/restore`, {
      method: 'PATCH',
      headers: getHeaders(),
    });
    if (!response.ok) {
      throw new Error('Failed to restore note');
    }
    return response.json();
  },

  async deleteNote(id: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/notes/${id}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!response.ok) {
      throw new Error('Failed to delete note');
    }
  },

  async triggerBulkImport(notes: CreateNoteRequest[]): Promise<any> {
    const response = await fetch(`${API_BASE_URL}/notes/import`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ notes }),
    });
    if (!response.ok) {
      throw new Error('Failed to trigger bulk import');
    }
    return response.json();
  },

  async getImportStatus(taskId: string): Promise<any> {
    const response = await fetch(`${API_BASE_URL}/notes/import/${taskId}`, {
      headers: getHeaders(),
    });
    if (!response.ok) {
      throw new Error('Failed to fetch import status');
    }
    return response.json();
  }
};
