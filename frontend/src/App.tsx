import React, { useState, useEffect } from 'react';
import type { Note } from './types';
import { notesApi, authApi } from './api/notesApi';
import { 
  Plus, 
  Trash2, 
  Archive, 
  Inbox, 
  Search, 
  Save, 
  RotateCcw, 
  FileText, 
  AlertCircle, 
  CheckCircle2,
  RefreshCw,
  LogOut,
  User as UserIcon,
  Lock,
  UploadCloud
} from 'lucide-react';

export default function App() {
  // Auth state
  // The JWT lives in an httpOnly cookie the browser attaches automatically, so the
  // frontend can't inspect it directly - session validity is checked via /auth/me.
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [authChecked, setAuthChecked] = useState(false);
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');

  // App workspace states
  const [notes, setNotes] = useState<Note[]>([]);
  const [selectedNote, setSelectedNote] = useState<Note | null>(null);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [query, setQuery] = useState('');
  const [view, setView] = useState<'active' | 'archived'>('active');
  
  // Status, logging & loading
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);

  // Load notes if authenticated
  const loadNotes = async (searchQuery = '') => {
    if (!isAuthenticated) return;
    setLoading(true);
    setError(null);
    try {
      let data: Note[];
      if (view === 'active') {
        data = await notesApi.getActiveNotes(searchQuery);
      } else {
        data = await notesApi.getArchivedNotes();
      }
      setNotes(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load notes');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    authApi.me()
      .then((user) => setIsAuthenticated(!!user))
      .finally(() => setAuthChecked(true));
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      loadNotes(query);
    }
  }, [view, query, isAuthenticated]);

  // Toast timers
  useEffect(() => {
    if (info) {
      const timer = setTimeout(() => setInfo(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [info]);

  useEffect(() => {
    if (error) {
      const timer = setTimeout(() => setError(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  // Auth operations
  const handleAuthSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      if (authMode === 'register') {
        await authApi.register(email, password, displayName);
        setInfo('Account created successfully');
      } else {
        await authApi.login(email, password);
        setInfo('Logged in successfully');
      }
      setIsAuthenticated(true);
    } catch (err: any) {
      setError(err.message || 'Authentication failed');
    }
  };

  const handleLogout = async () => {
    await authApi.logout();
    setIsAuthenticated(false);
    setNotes([]);
    setSelectedNote(null);
    setInfo('Logged out');
  };

  const handleSelectNote = (note: Note) => {
    setSelectedNote(note);
    setTitle(note.title);
    setContent(note.content || '');
  };

  const handleCreateNew = () => {
    setSelectedNote(null);
    setTitle('');
    setContent('');
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      setError('Title cannot be blank');
      return;
    }

    try {
      if (selectedNote) {
        const updated = await notesApi.updateNote(selectedNote.id, { title, content });
        setInfo('Note updated successfully');
        handleSelectNote(updated);
      } else {
        const created = await notesApi.createNote({ title, content });
        setInfo('Note created successfully');
        handleSelectNote(created);
      }
      loadNotes(query);
    } catch (err: any) {
      setError(err.message || 'Error saving note');
    }
  };

  const handleArchive = async (id: string) => {
    try {
      await notesApi.archiveNote(id);
      setInfo('Note archived');
      if (selectedNote?.id === id) {
        handleCreateNew();
      }
      loadNotes(query);
    } catch (err: any) {
      setError(err.message || 'Failed to archive note');
    }
  };

  const handleRestore = async (id: string) => {
    try {
      await notesApi.restoreNote(id);
      setInfo('Note restored');
      if (selectedNote?.id === id) {
        handleCreateNew();
      }
      loadNotes(query);
    } catch (err: any) {
      setError(err.message || 'Failed to restore note');
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to permanently delete this note?')) return;
    try {
      await notesApi.deleteNote(id);
      setInfo('Note permanently deleted');
      if (selectedNote?.id === id) {
        handleCreateNew();
      }
      loadNotes(query);
    } catch (err: any) {
      setError(err.message || 'Failed to delete note');
    }
  };

  // Demo Bulk Import Action
  const handleBulkImportDemo = async () => {
    setImporting(true);
    setInfo('Starting bulk import of 5 notes...');
    try {
      const mockNotes = Array.from({ length: 5 }, (_, i) => ({
        title: `Bulk Note #${i + 1}`,
        content: `Automatically imported markdown content for bulk note item #${i + 1}.`
      }));
      const task = await notesApi.triggerBulkImport(mockNotes);
      
      // Poll status
      const interval = setInterval(async () => {
        try {
          const status = await notesApi.getImportStatus(task.taskId);
          if (status.status === 'COMPLETED') {
            clearInterval(interval);
            setInfo(`Successfully imported ${status.totalCount} notes!`);
            setImporting(false);
            loadNotes(query);
          } else if (status.status === 'FAILED') {
            clearInterval(interval);
            setError('Bulk import failed.');
            setImporting(false);
          }
        } catch (e) {
          clearInterval(interval);
          setImporting(false);
        }
      }, 1000);
    } catch (err: any) {
      setError(err.message || 'Bulk import failed');
      setImporting(false);
    }
  };

  // Wait for the session check before deciding which screen to show, so an
  // already-logged-in user doesn't flash the login form on refresh.
  if (!authChecked) {
    return null;
  }

  // Login/Register Screen
  if (!isAuthenticated) {
    return (
      <div style={styles.authContainer}>
        {info && (
          <div style={{ ...styles.toast, ...styles.toastInfo }}>
            <CheckCircle2 size={18} />
            <span>{info}</span>
          </div>
        )}
        {error && (
          <div style={{ ...styles.toast, ...styles.toastError }}>
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}
        
        <div style={styles.authCard}>
          <div style={styles.authHeader}>
            <FileText size={32} color="#6366f1" />
            <h2 style={styles.authTitle}>CloudNotes Platform</h2>
            <p style={styles.authSubtitle}>Distributed document microservices client</p>
          </div>
          
          <form onSubmit={handleAuthSubmit} style={styles.authForm}>
            {authMode === 'register' && (
              <div style={styles.inputGroup}>
                <label style={styles.label}>Display Name</label>
                <div style={styles.inputWrapper}>
                  <UserIcon size={16} style={styles.inputIcon} />
                  <input 
                    type="text" 
                    placeholder="John Doe" 
                    value={displayName} 
                    onChange={(e) => setDisplayName(e.target.value)} 
                    style={styles.authInput}
                    required
                  />
                </div>
              </div>
            )}
            
            <div style={styles.inputGroup}>
              <label style={styles.label}>Email Address</label>
              <div style={styles.inputWrapper}>
                <UserIcon size={16} style={styles.inputIcon} />
                <input 
                  type="email" 
                  placeholder="name@domain.com" 
                  value={email} 
                  onChange={(e) => setEmail(e.target.value)} 
                  style={styles.authInput}
                  required
                />
              </div>
            </div>

            <div style={styles.inputGroup}>
              <label style={styles.label}>Password</label>
              <div style={styles.inputWrapper}>
                <Lock size={16} style={styles.inputIcon} />
                <input 
                  type="password" 
                  placeholder="••••••••" 
                  value={password} 
                  onChange={(e) => setPassword(e.target.value)} 
                  style={styles.authInput}
                  required
                />
              </div>
            </div>

            <button type="submit" style={styles.authButton}>
              {authMode === 'login' ? 'Sign In' : 'Create Account'}
            </button>
          </form>

          <div style={styles.authFooter}>
            {authMode === 'login' ? (
              <p>Don't have an account? <span style={styles.authLink} onClick={() => setAuthMode('register')}>Sign up</span></p>
            ) : (
              <p>Already have an account? <span style={styles.authLink} onClick={() => setAuthMode('login')}>Sign in</span></p>
            )}
          </div>
        </div>
      </div>
    );
  }

  // Authenticated workspace
  return (
    <div style={styles.appContainer}>
      {/* Toast notifications */}
      {info && (
        <div style={{ ...styles.toast, ...styles.toastInfo }}>
          <CheckCircle2 size={18} />
          <span>{info}</span>
        </div>
      )}
      {error && (
        <div style={{ ...styles.toast, ...styles.toastError }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Sidebar */}
      <div style={styles.sidebar}>
        <div style={styles.sidebarHeader}>
          <div style={styles.logoRow}>
            <div style={styles.logo}>
              <FileText size={24} color="#6366f1" />
              <h1 style={styles.title}>CloudNotes</h1>
            </div>
            <button style={styles.logoutBtn} onClick={handleLogout} title="Sign Out">
              <LogOut size={16} />
            </button>
          </div>
          <button style={styles.newButton} onClick={handleCreateNew}>
            <Plus size={16} /> New Note
          </button>
        </div>

        {/* View Switcher */}
        <div style={styles.tabs}>
          <button 
            style={{ ...styles.tab, ...(view === 'active' ? styles.tabActive : {}) }}
            onClick={() => { setView('active'); handleCreateNew(); }}
          >
            <Inbox size={16} /> Notes
          </button>
          <button 
            style={{ ...styles.tab, ...(view === 'archived' ? styles.tabActive : {}) }}
            onClick={() => { setView('archived'); handleCreateNew(); }}
          >
            <Archive size={16} /> Archive
          </button>
        </div>

        {/* High Throughput Import Button Demo */}
        <div style={styles.bulkWrapper}>
          <button 
            style={styles.bulkButton} 
            onClick={handleBulkImportDemo} 
            disabled={importing}
          >
            {importing ? (
              <>
                <RefreshCw size={14} className="spin" style={styles.spinner} /> Importing...
              </>
            ) : (
              <>
                <UploadCloud size={14} /> Bulk Import (High Load Demo)
              </>
            )}
          </button>
        </div>

        {/* Search */}
        {view === 'active' && (
          <div style={styles.searchWrapper}>
            <Search size={16} style={styles.searchIcon} />
            <input 
              type="text" 
              placeholder="Search notes..." 
              value={query} 
              onChange={(e) => setQuery(e.target.value)} 
              style={styles.searchInput}
            />
          </div>
        )}

        {/* Notes List */}
        <div style={styles.notesList}>
          {loading && (
            <div style={styles.loadingSpinner}>
              <RefreshCw size={24} className="spin" style={styles.spinner} />
              <p>Loading...</p>
            </div>
          )}
          {!loading && notes.length === 0 && (
            <div style={styles.emptyState}>
              <p>No notes found</p>
            </div>
          )}
          {!loading && notes.map(note => (
            <div 
              key={note.id} 
              style={{
                ...styles.noteItem,
                ...(selectedNote?.id === note.id ? styles.noteItemActive : {})
              }}
              onClick={() => handleSelectNote(note)}
            >
              <h3 style={styles.noteItemTitle}>{note.title || 'Untitled Note'}</h3>
              <p style={styles.noteItemBody}>
                {note.content ? note.content.substring(0, 80) : 'No content'}
              </p>
              <div style={styles.noteItemMeta}>
                <span>Updated {new Date(note.updatedAt).toLocaleDateString()}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Main Workspace */}
      <div style={styles.workspace}>
        <form onSubmit={handleSave} style={styles.form}>
          <div style={styles.workspaceHeader}>
            <input 
              type="text" 
              placeholder="Note Title" 
              value={title} 
              onChange={(e) => setTitle(e.target.value)} 
              style={styles.titleInput}
            />
            <div style={styles.headerActions}>
              {selectedNote && (
                <>
                  {selectedNote.archived ? (
                    <button 
                      type="button" 
                      onClick={() => handleRestore(selectedNote.id)}
                      style={styles.actionButton}
                      title="Restore Note"
                    >
                      <RotateCcw size={16} /> Restore
                    </button>
                  ) : (
                    <button 
                      type="button" 
                      onClick={() => handleArchive(selectedNote.id)}
                      style={styles.actionButton}
                      title="Archive Note"
                    >
                      <Archive size={16} /> Archive
                    </button>
                  )}
                  <button 
                    type="button" 
                    onClick={() => handleDelete(selectedNote.id)}
                    style={{ ...styles.actionButton, ...styles.deleteButton }}
                    title="Delete Note"
                  >
                    <Trash2 size={16} /> Delete
                  </button>
                </>
              )}
              <button type="submit" style={styles.saveButton}>
                <Save size={16} /> Save
              </button>
            </div>
          </div>
          <div style={styles.editorArea}>
            <textarea 
              placeholder="Write your note content here..." 
              value={content} 
              onChange={(e) => setContent(e.target.value)} 
              style={styles.textArea}
            />
          </div>
        </form>
      </div>
    </div>
  );
}

const styles: { [key: string]: React.CSSProperties } = {
  appContainer: {
    display: 'flex',
    height: '100vh',
    width: '100vw',
    backgroundColor: '#0f172a',
    color: '#e2e8f0',
    overflow: 'hidden',
  },
  authContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    height: '100vh',
    width: '100vw',
    backgroundColor: '#0b0f19',
    color: '#e2e8f0',
  },
  authCard: {
    width: '100%',
    maxWidth: '420px',
    padding: '40px',
    borderRadius: '16px',
    backgroundColor: '#0f172a',
    border: '1px solid #1e293b',
    boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.3), 0 8px 10px -6px rgba(0, 0, 0, 0.3)',
  },
  authHeader: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginBottom: '30px',
    textAlign: 'center',
  },
  authTitle: {
    fontSize: '1.5rem',
    fontWeight: 700,
    margin: '15px 0 5px 0',
    background: 'linear-gradient(135deg, #818cf8 0%, #6366f1 100%)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
  },
  authSubtitle: {
    fontSize: '0.85rem',
    color: '#64748b',
    margin: 0,
  },
  authForm: {
    display: 'flex',
    flexDirection: 'column',
    gap: '20px',
  },
  inputGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
  },
  label: {
    fontSize: '0.85rem',
    fontWeight: 500,
    color: '#94a3b8',
  },
  inputWrapper: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
  },
  inputIcon: {
    position: 'absolute',
    left: '12px',
    color: '#64748b',
  },
  authInput: {
    width: '100%',
    padding: '12px 12px 12px 40px',
    backgroundColor: '#1e293b',
    border: '1px solid #334155',
    borderRadius: '8px',
    color: '#f8fafc',
    outline: 'none',
    boxSizing: 'border-box',
    fontSize: '0.95rem',
  },
  authButton: {
    padding: '12px',
    backgroundColor: '#6366f1',
    color: '#ffffff',
    border: 'none',
    borderRadius: '8px',
    fontWeight: 600,
    fontSize: '0.95rem',
    cursor: 'pointer',
    transition: 'background-color 0.2s',
    marginTop: '10px',
  },
  authFooter: {
    marginTop: '25px',
    textAlign: 'center',
    fontSize: '0.85rem',
    color: '#94a3b8',
  },
  authLink: {
    color: '#818cf8',
    cursor: 'pointer',
    fontWeight: 500,
  },
  logoRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  logoutBtn: {
    backgroundColor: 'transparent',
    border: 'none',
    color: '#64748b',
    cursor: 'pointer',
    padding: '8px',
    borderRadius: '6px',
    transition: 'color 0.2s, background-color 0.2s',
    display: 'flex',
    alignItems: 'center',
  },
  toast: {
    position: 'fixed',
    top: '20px',
    right: '20px',
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
    padding: '12px 20px',
    borderRadius: '8px',
    zIndex: 1000,
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.5)',
    fontWeight: 500,
    fontSize: '0.9rem',
  },
  toastInfo: {
    backgroundColor: '#059669',
    color: '#ecfdf5',
  },
  toastError: {
    backgroundColor: '#dc2626',
    color: '#fef2f2',
  },
  sidebar: {
    width: '320px',
    borderRight: '1px solid #1e293b',
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: '#0f172a',
  },
  sidebarHeader: {
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    gap: '15px',
  },
  logo: {
    display: 'flex',
    alignItems: 'center',
    gap: '10px',
  },
  title: {
    fontSize: '1.25rem',
    fontWeight: 700,
    margin: 0,
    background: 'linear-gradient(135deg, #818cf8 0%, #6366f1 100%)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
  },
  newButton: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    padding: '10px',
    backgroundColor: '#6366f1',
    color: '#ffffff',
    border: 'none',
    borderRadius: '8px',
    fontWeight: 600,
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  },
  tabs: {
    display: 'flex',
    padding: '0 20px 10px 20px',
    gap: '10px',
  },
  tab: {
    flex: 1,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '6px',
    padding: '8px',
    backgroundColor: 'transparent',
    color: '#94a3b8',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
    fontWeight: 500,
  },
  tabActive: {
    backgroundColor: '#1e293b',
    color: '#e2e8f0',
  },
  bulkWrapper: {
    padding: '0 20px 15px 20px',
  },
  bulkButton: {
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '8px',
    padding: '8px 10px',
    backgroundColor: '#1e293b',
    color: '#38bdf8',
    border: '1px solid #0284c7',
    borderRadius: '6px',
    fontSize: '0.85rem',
    fontWeight: 500,
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  },
  searchWrapper: {
    position: 'relative',
    margin: '0 20px 15px 20px',
  },
  searchIcon: {
    position: 'absolute',
    left: '10px',
    top: '50%',
    transform: 'translateY(-50%)',
    color: '#64748b',
  },
  searchInput: {
    width: '100%',
    padding: '8px 10px 8px 34px',
    backgroundColor: '#1e293b',
    border: '1px solid #334155',
    borderRadius: '6px',
    color: '#f8fafc',
    outline: 'none',
    boxSizing: 'border-box',
  },
  notesList: {
    flex: 1,
    overflowY: 'auto',
    padding: '0 20px 20px 20px',
  },
  noteItem: {
    padding: '15px',
    borderRadius: '8px',
    backgroundColor: '#1e293b',
    border: '1px solid #334155',
    marginBottom: '10px',
    cursor: 'pointer',
    transition: 'transform 0.2s, background-color 0.2s',
  },
  noteItemActive: {
    backgroundColor: '#312e81',
    borderColor: '#4f46e5',
  },
  noteItemTitle: {
    fontSize: '0.95rem',
    fontWeight: 600,
    margin: '0 0 5px 0',
    color: '#f8fafc',
  },
  noteItemBody: {
    fontSize: '0.85rem',
    color: '#94a3b8',
    margin: '0 0 10px 0',
    lineHeight: '1.4',
  },
  noteItemMeta: {
    fontSize: '0.75rem',
    color: '#64748b',
  },
  loadingSpinner: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '40px 0',
    color: '#94a3b8',
  },
  spinner: {
    animation: 'spin 1s linear infinite',
  },
  emptyState: {
    textAlign: 'center',
    padding: '40px 0',
    color: '#64748b',
  },
  workspace: {
    flex: 1,
    backgroundColor: '#0b0f19',
    display: 'flex',
    flexDirection: 'column',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
  },
  workspaceHeader: {
    padding: '20px',
    borderBottom: '1px solid #1e293b',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '20px',
  },
  titleInput: {
    flex: 1,
    background: 'transparent',
    border: 'none',
    fontSize: '1.5rem',
    fontWeight: 700,
    color: '#f8fafc',
    outline: 'none',
  },
  headerActions: {
    display: 'flex',
    gap: '10px',
  },
  actionButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    padding: '8px 14px',
    backgroundColor: '#1e293b',
    color: '#e2e8f0',
    border: '1px solid #334155',
    borderRadius: '6px',
    cursor: 'pointer',
    fontWeight: 500,
    fontSize: '0.85rem',
  },
  deleteButton: {
    backgroundColor: '#7f1d1d',
    borderColor: '#991b1b',
    color: '#fca5a5',
  },
  saveButton: {
    display: 'flex',
    alignItems: 'center',
    gap: '6px',
    padding: '8px 16px',
    backgroundColor: '#4f46e5',
    color: '#ffffff',
    border: 'none',
    borderRadius: '6px',
    cursor: 'pointer',
    fontWeight: 600,
    fontSize: '0.85rem',
  },
  editorArea: {
    flex: 1,
    padding: '20px',
  },
  textArea: {
    width: '100%',
    height: '100%',
    backgroundColor: 'transparent',
    border: 'none',
    resize: 'none',
    color: '#e2e8f0',
    fontSize: '1.05rem',
    lineHeight: '1.6',
    outline: 'none',
    fontFamily: 'inherit',
  },
};
