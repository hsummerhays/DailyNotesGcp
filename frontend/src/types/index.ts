export interface Note {
  id: string;
  title: string;
  content: string;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CreateNoteRequest {
  title: string;
  content: string;
}

export interface UpdateNoteRequest {
  title: string;
  content: string;
}
