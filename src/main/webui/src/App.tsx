import { useState, useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { ConfirmDialog } from './ConfirmDialog';
import { Copy, Check } from 'lucide-react';
import './App.css';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  tps?: number;
}

interface Conversation {
  id: string;
  title: string;
  messages: ChatMessage[];
  model: string;
}

interface ToolInfoDTO {
  name: string;
  displayName: string;
  description: string;
  enabled: boolean;
}

function App() {
  const [models, setModels] = useState<string[]>([]);
  const [tools, setTools] = useState<ToolInfoDTO[]>([]);
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [conversations, setConversations] = useState<Conversation[]>(() => {
    const saved = localStorage.getItem('ollama4j_conversations');
    return saved ? JSON.parse(saved) : [];
  });
  const [currentConversationId, setCurrentConversationId] = useState<string | null>(null);
  const [currentPath, setCurrentPath] = useState(window.location.pathname);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [hostUrl, setHostUrl] = useState('');
  const [settingsModels, setSettingsModels] = useState<string[]>([]);
  const [loadedModels, setLoadedModels] = useState<string[]>([]);
  const [modelDetails, setModelDetails] = useState<Record<string, any>>({});
  const [isServerReachable, setIsServerReachable] = useState<boolean | null>(null);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [chatToDelete, setChatToDelete] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);
  const [modelSearch, setModelSearch] = useState('');
  const [isModelDropdownOpen, setIsModelDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const filteredModels = models.filter(m => 
    m.toLowerCase().includes(modelSearch.toLowerCase())
  );

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsModelDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const currentConversation = conversations.find(c => c.id === currentConversationId);
  const messages = currentConversation ? currentConversation.messages : [];

  const fetchTools = () => {
    fetch('/api/settings/tools')
      .then(res => {
        if (!res.ok) throw new Error("Failed to fetch tools");
        return res.json();
      })
      .then(data => setTools(data))
      .catch(err => console.error("Error fetching tools", err));
  };

  const toggleTool = (toolName: string) => {
    fetch(`/api/settings/tools/${encodeURIComponent(toolName)}/toggle`, {
      method: 'POST'
    })
      .then(res => {
        if (!res.ok) throw new Error("Failed to toggle tool");
        return res.json();
      })
      .then(data => setTools(data))
      .catch(err => console.error("Error toggling tool", err));
  };

  const checkHostReachability = () => {
    fetch('/api/settings/ping')
      .then(res => {
        if (!res.ok) throw new Error("Ping failed");
        return res.json();
      })
      .then(data => setIsServerReachable(!!data.reachable))
      .catch(err => {
        console.error("Error pinging server", err);
        setIsServerReachable(false);
      });
  };

  const fetchModelDetails = (modelName: string) => {
    fetch(`/api/models/details/${encodeURIComponent(modelName)}`)
      .then(res => {
        if (!res.ok) throw new Error("Failed to fetch details");
        return res.json();
      })
      .then(data => {
        setModelDetails(prev => ({
          ...prev,
          [modelName]: data
        }));
      })
      .catch(err => console.error("Error fetching details for " + modelName, err));
  };

  const fetchModels = () => {
    fetch('/api/models')
      .then(res => {
        if (!res.ok) throw new Error("Server error: " + res.status);
        return res.json();
      })
      .then(data => {
        setModels(data);
        setSettingsModels(data);
        if (data && data.length > 0) {
          data.forEach((m: string) => fetchModelDetails(m));
          if (currentConversation && currentConversation.model) {
            setSelectedModel(currentConversation.model);
          } else {
            setSelectedModel(data[0]);
          }
        } else {
          setSelectedModel('');
        }
      })
      .catch(err => {
        console.error("Failed to fetch models", err);
        setModels([]);
        setSettingsModels([]);
      });
  };

  useEffect(() => {
    fetchModels();
  }, []);

  useEffect(() => {
    localStorage.setItem('ollama4j_conversations', JSON.stringify(conversations));
  }, [conversations]);

  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname);
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigateTo = (path: string) => {
    window.history.pushState({}, '', path);
    setCurrentPath(path);
  };

  useEffect(() => {
    if (currentPath === '/settings') {
      fetch('/api/settings/host')
        .then(res => res.text())
        .then(data => setHostUrl(data))
        .catch(err => console.error("Failed to fetch host", err));
      fetchModels();
      fetchLoadedModels();
      checkHostReachability();
      fetchTools();
    } else if (currentPath.startsWith('/chat/')) {
      const uuid = currentPath.substring(6);
      const existing = conversations.find(c => c.id === uuid);
      if (existing) {
        setCurrentConversationId(uuid);
        if (existing.model) {
          setSelectedModel(existing.model);
        }
      } else {
        const newConv: Conversation = {
          id: uuid,
          title: 'New Chat',
          messages: [],
          model: selectedModel || models[0] || '',
        };
        setConversations(prev => [newConv, ...prev]);
        setCurrentConversationId(uuid);
      }
    } else {
      if (conversations.length > 0) {
        navigateTo(`/chat/${conversations[0].id}`);
      } else {
        const newUuid = crypto.randomUUID();
        navigateTo(`/chat/${newUuid}`);
      }
    }
  }, [currentPath]);

  useEffect(() => {
    if (currentPath !== '/settings') return;

    const loadedInterval = setInterval(() => {
      fetchLoadedModels();
    }, 1000);

    const pingInterval = setInterval(() => {
      checkHostReachability();
    }, 3000);

    return () => {
      clearInterval(loadedInterval);
      clearInterval(pingInterval);
    };
  }, [currentPath]);

  const startNewChat = () => {
    if (currentConversation && currentConversation.messages.length === 0) {
      return;
    }
    const newUuid = crypto.randomUUID();
    navigateTo(`/chat/${newUuid}`);
  };

  const deleteConversation = (id: string, event: React.MouseEvent) => {
    event.stopPropagation();
    setChatToDelete(id);
    setIsConfirmOpen(true);
  };

  const handleConfirmDelete = () => {
    if (!chatToDelete) return;
    const id = chatToDelete;
    const updated = conversations.filter(c => c.id !== id);
    setConversations(updated);

    if (id === currentConversationId) {
      if (updated.length > 0) {
        navigateTo(`/chat/${updated[0].id}`);
      } else {
        const newUuid = crypto.randomUUID();
        navigateTo(`/chat/${newUuid}`);
      }
    }
    setIsConfirmOpen(false);
    setChatToDelete(null);
  };

  const handleCancelDelete = () => {
    setIsConfirmOpen(false);
    setChatToDelete(null);
  };

  const closeSettings = () => {
    if (currentConversationId) {
      navigateTo(`/chat/${currentConversationId}`);
    } else if (conversations.length > 0) {
      navigateTo(`/chat/${conversations[0].id}`);
    } else {
      navigateTo('/');
    }
  };

  const openSettings = () => {
    navigateTo('/settings');
  };

  const saveSettings = () => {
    fetch('/api/settings/host', {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: hostUrl
    })
      .then(res => {
        if (res.ok) {
          fetchModels();
          closeSettings();
        } else {
          console.error("Failed to save host");
        }
      })
      .catch(err => console.error("Failed to save host", err));
  };

  const deleteModel = (modelName: string) => {
    fetch(`/api/models/${modelName}`, {
      method: 'DELETE'
    })
      .then(res => {
        if (res.ok) {
          fetchModels();
        } else {
          console.error("Failed to delete model");
        }
      })
      .catch(err => console.error("Failed to delete model", err));
  };

  const fetchLoadedModels = () => {
    fetch('/api/models/loaded')
      .then(res => {
        if (!res.ok) throw new Error("Server error: " + res.status);
        return res.json();
      })
      .then(data => {
        setLoadedModels(data);
        if (data) {
          data.forEach((m: string) => fetchModelDetails(m));
        }
      })
      .catch(err => {
        console.error("Failed to fetch currently running models", err);
        setLoadedModels([]);
      });
  };

  const unloadModel = (modelName: string) => {
    fetch(`/api/models/unload/${modelName}`, {
      method: 'POST'
    })
      .then(res => {
        if (res.ok) {
          fetchLoadedModels();
        } else {
          console.error("Failed to unload model");
        }
      })
      .catch(err => console.error("Failed to unload model", err));
  };

  const copyToClipboard = (text: string, index: number) => {
    navigator.clipboard.writeText(text)
      .then(() => {
        setCopiedIndex(index);
        setTimeout(() => setCopiedIndex(null), 2000);
      })
      .catch(err => console.error("Failed to copy text", err));
  };

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const stopResponseGeneration = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setIsLoading(false);
  };

  const sendMessage = async () => {
    if (!inputMessage.trim() || !selectedModel || !currentConversationId) return;

    const controller = new AbortController();
    abortControllerRef.current = controller;

    const userMessage = inputMessage;
    setInputMessage('');
    setIsLoading(true);

    setConversations(prev => prev.map(c => {
      if (c.id === currentConversationId) {
        const updatedMessages: ChatMessage[] = [
          ...c.messages,
          { role: 'user', content: userMessage },
          { role: 'assistant', content: '' }
        ];
        const title = c.title === 'New Chat' ? (userMessage.length > 30 ? userMessage.substring(0, 30) + '...' : userMessage) : c.title;
        return { ...c, messages: updatedMessages, title, model: selectedModel };
      }
      return c;
    }));

    try {
      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: selectedModel,
          message: userMessage
        }),
        signal: controller.signal
      });

      if (response.body) {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let totalChars = 0;
        const startTime = Date.now();

        while (true) {
          const { value, done } = await reader.read();
          if (done) break;
          const chunk = decoder.decode(value);
          if (chunk) {
            totalChars += chunk.length;
            const elapsedSeconds = (Date.now() - startTime) / 1000;
            let currentTps: number | undefined;
            if (elapsedSeconds > 0.05) {
              const approxTokens = totalChars / 4;
              currentTps = approxTokens / elapsedSeconds;
            }
            setConversations(prev => prev.map(c => {
              if (c.id === currentConversationId) {
                const updated = [...c.messages];
                if (updated.length > 0) {
                  updated[updated.length - 1] = {
                    ...updated[updated.length - 1],
                    content: updated[updated.length - 1].content + chunk,
                    ...(currentTps !== undefined ? { tps: currentTps } : {})
                  };
                }
                return { ...c, messages: updated };
              }
              return c;
            }));
          }
        }
      }
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        console.log("Generation stopped by user.");
      } else {
        console.error("Chat error:", error);
        setConversations(prev => prev.map(c => {
          if (c.id === currentConversationId) {
            const updated = [...c.messages];
            if (updated.length > 0) {
              updated[updated.length - 1] = {
                ...updated[updated.length - 1],
                content: "Error connecting to server."
              };
            }
            return { ...c, messages: updated };
          }
          return c;
        }));
      }
    } finally {
      setIsLoading(false);
      abortControllerRef.current = null;
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      sendMessage();
    }
  };

  return (
    <div className="app-container">
      <div className="sidebar">
        <div className="logo-container">
          <h2>Ollama4j UI</h2>
        </div>
        
        <button 
          className="new-chat-btn" 
          onClick={startNewChat}
          disabled={!!(currentConversation && currentConversation.messages.length === 0)}
        >
          + New Chat
        </button>

        <div className="conversations-list-section">
          <label>Recent Chats</label>
          <div className="conversations-list">
            {conversations.map(conv => (
              <div 
                key={conv.id} 
                className={`conversation-item ${conv.id === currentConversationId ? 'active' : ''}`}
                onClick={() => navigateTo(`/chat/${conv.id}`)}
              >
                <span className="chat-icon">💬</span>
                <span className="chat-title" title={conv.title}>{conv.title}</span>
                <button 
                  className="delete-chat-btn" 
                  onClick={(e) => deleteConversation(conv.id, e)}
                  title="Delete chat"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    <line x1="10" y1="11" x2="10" y2="17"></line>
                    <line x1="14" y1="11" x2="14" y2="17"></line>
                  </svg>
                </button>
              </div>
            ))}
          </div>
        </div>

        <button className="settings-button" onClick={openSettings}>
          ⚙️ Settings
        </button>
      </div>
      
      {currentPath === '/settings' ? (
        <div className="settings-page">
          <div className="settings-header">
            <h1>Settings</h1>
            <button className="settings-back-btn" onClick={closeSettings}>
              ← Back to Chat
            </button>
          </div>
          
          <div className="settings-content-wrapper">
            <div className="settings-group">
              <div className="running-header-container">
                <label>Ollama Host Address</label>
                <div className={`live-indicator ${isServerReachable === true ? 'online' : isServerReachable === false ? 'offline' : 'checking'}`}>
                  {isServerReachable !== null && <span className={`ping-dot ${isServerReachable ? 'online' : 'offline'}`}></span>}
                  <span className="live-text">
                    {isServerReachable === true 
                      ? "Live" 
                      : isServerReachable === false 
                        ? "Not Reachable" 
                        : "Checking..."}
                  </span>
                </div>
              </div>
              <div className="settings-input-group">
                <input 
                  type="text" 
                  value={hostUrl} 
                  onChange={e => setHostUrl(e.target.value)} 
                  placeholder="http://localhost:11434"
                />
                <button className="save-button" onClick={saveSettings}>Save</button>
              </div>
            </div>

            <div className="settings-group">
              <label>AI Assistant Tools</label>
              <div style={{ color: '#8e8ea0', fontSize: '0.85rem', marginBottom: '15px', lineHeight: '1.4' }}>
                Enable or disable capabilities that the AI assistant can call to perform system actions on your computer.
              </div>
              <div className="tools-list">
                {tools.length === 0 ? (
                  <div style={{ color: '#8e8ea0', padding: '10px' }}>No tools registered.</div>
                ) : (
                  tools.map(t => (
                    <div className="tool-item" key={t.name}>
                      <div className="tool-info-container">
                        <div className="tool-name-row">
                          <span className="tool-name">{t.displayName}</span>
                          <span className={`tool-status-badge ${t.enabled ? 'enabled' : 'disabled'}`}>
                            {t.enabled ? 'Active' : 'Inactive'}
                          </span>
                        </div>
                        <p className="tool-description">{t.description}</p>
                      </div>
                      <button 
                        className={`toggle-tool-button ${t.enabled ? 'enabled' : 'disabled'}`}
                        onClick={() => toggleTool(t.name)}
                      >
                        {t.enabled ? 'Disable' : 'Enable'}
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="settings-group">
              <label>Manage Models</label>
              <div className="model-list">
                {settingsModels.length === 0 ? (
                  <div style={{ color: '#8e8ea0', padding: '10px' }}>No models found.</div>
                ) : (
                  settingsModels.map(m => (
                    <div className="model-item" key={m}>
                      <div className="model-info-container">
                        <span className="model-name">{m}</span>
                        {modelDetails[m] && (
                          <div className="model-details-badge-container">
                            {modelDetails[m].details?.family && <span className="model-badge family">Family: {modelDetails[m].details.family}</span>}
                            {modelDetails[m].details?.parameter_size && <span className="model-badge params">Params: {modelDetails[m].details.parameter_size}</span>}
                            {modelDetails[m].details?.quantization_level && <span className="model-badge quantization">Quantization: {modelDetails[m].details.quantization_level}</span>}
                            {modelDetails[m].details?.format && <span className="model-badge format">Format: {modelDetails[m].details.format}</span>}
                          </div>
                        )}
                      </div>
                      <button className="delete-button" onClick={() => deleteModel(m)}>Delete</button>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="settings-group">
              <div className="running-header-container">
                <label>Models currently running</label>
                <div className="live-indicator">
                  <span className="ping-dot"></span>
                  <span className="live-text">Live (Auto-refreshes every 1s)</span>
                </div>
              </div>
              <div className="model-list">
                {loadedModels.length === 0 ? (
                  <div style={{ color: '#8e8ea0', padding: '10px' }}>No models loaded in memory.</div>
                ) : (
                  loadedModels.map(m => (
                    <div className="model-item" key={m}>
                      <div className="model-info-container">
                        <span className="model-name">{m}</span>
                        {modelDetails[m] && (
                          <div className="model-details-badge-container">
                            {modelDetails[m].details?.family && <span className="model-badge family">Family: {modelDetails[m].details.family}</span>}
                            {modelDetails[m].details?.parameter_size && <span className="model-badge params">Params: {modelDetails[m].details.parameter_size}</span>}
                            {modelDetails[m].details?.quantization_level && <span className="model-badge quantization">Quantization: {modelDetails[m].details.quantization_level}</span>}
                            {modelDetails[m].details?.format && <span className="model-badge format">Format: {modelDetails[m].details.format}</span>}
                          </div>
                        )}
                      </div>
                      <button className="delete-button" onClick={() => unloadModel(m)}>Unload</button>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="main-chat">
          <div className="chat-messages">
            {messages.length === 0 ? (
              <div className="empty-state">
                <h1>What can I help you with?</h1>
                <p>Select a model and start chatting.</p>
              </div>
            ) : (
              messages.map((msg, i) => (
                <div key={i} className={`message-row ${msg.role}`}>
                  <div className="message-container">
                    <div className={`avatar ${msg.role}`}>
                      {msg.role === 'user' ? '👤' : '🤖'}
                    </div>
                    <div className={`message-bubble ${msg.role}`}>
                      <ReactMarkdown remarkPlugins={[remarkGfm]}>
                        {msg.content}
                      </ReactMarkdown>
                    </div>
                    <div className="message-actions">
                      {msg.role === 'assistant' && msg.tps !== undefined && (
                        <span className="tps-indicator">
                          ⚡ {msg.tps.toFixed(1)} tok/s
                        </span>
                      )}
                      <button 
                        className="copy-message-btn" 
                        onClick={() => copyToClipboard(msg.content, i)}
                        title="Copy message"
                      >
                        {copiedIndex === i ? (
                          <Check className="copy-icon" size={16} />
                        ) : (
                          <Copy className="copy-icon" size={16} />
                        )}
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
            <div ref={messagesEndRef} />
          </div>
          
          <div className="chat-input-container">
            <input 
              type="text" 
              placeholder="Type a message..." 
              value={inputMessage}
              onChange={e => setInputMessage(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={isLoading || !selectedModel}
              className="chat-input"
            />
            
            <div className="searchable-model-container" ref={dropdownRef}>
              <button 
                type="button"
                className="model-dropdown-trigger"
                onClick={() => setIsModelDropdownOpen(!isModelDropdownOpen)}
                disabled={models.length === 0}
              >
                <span className="selected-model-text">
                  {selectedModel || 'Select Model'}
                </span>
                <span className="chevron-icon">▼</span>
              </button>
              
              {isModelDropdownOpen && (
                <div className="model-dropdown-menu">
                  <input 
                    type="text"
                    placeholder="Search models..."
                    value={modelSearch}
                    onChange={e => setModelSearch(e.target.value)}
                    className="model-search-input"
                    autoFocus
                  />
                  <div className="model-options-list">
                    {filteredModels.length === 0 ? (
                      <div className="no-models-found">No models found</div>
                    ) : (
                      filteredModels.map(m => (
                        <div 
                          key={m} 
                          className={`model-option-item ${m === selectedModel ? 'active' : ''}`}
                          onClick={() => {
                            setSelectedModel(m);
                            if (currentConversationId) {
                              setConversations(prev => prev.map(c => {
                                if (c.id === currentConversationId) {
                                  return { ...c, model: m };
                                }
                                return c;
                              }));
                            }
                            setIsModelDropdownOpen(false);
                            setModelSearch('');
                          }}
                        >
                          {m}
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>

            {isLoading ? (
              <button 
                onClick={stopResponseGeneration} 
                className="stop-button"
              >
                <span className="stop-icon">■</span> Stop
              </button>
            ) : (
              <button 
                onClick={sendMessage} 
                disabled={!inputMessage.trim() || !selectedModel}
                className="send-button"
              >
                Send
              </button>
            )}
          </div>
        </div>
      )}
      <ConfirmDialog
        isOpen={isConfirmOpen}
        title="Delete Chat"
        message="Are you sure you want to delete this conversation? This action cannot be undone."
        onConfirm={handleConfirmDelete}
        onCancel={handleCancelDelete}
      />
    </div>
  );
}

export default App;
