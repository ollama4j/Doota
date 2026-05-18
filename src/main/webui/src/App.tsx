import { useState, useEffect, useRef } from 'react';
import './App.css';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

function App() {
  const [models, setModels] = useState<string[]>([]);
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [hostUrl, setHostUrl] = useState('');
  const [settingsModels, setSettingsModels] = useState<string[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);

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
          setSelectedModel(data[0]);
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

  const openSettings = () => {
    fetch('/api/settings/host')
      .then(res => res.text())
      .then(data => setHostUrl(data))
      .catch(err => console.error("Failed to fetch host", err));
    fetchModels();
    setShowSettings(true);
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
          setShowSettings(false);
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

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async () => {
    if (!inputMessage.trim() || !selectedModel) return;

    const userMessage = inputMessage;
    setMessages(prev => [...prev, { role: 'user', content: userMessage }]);
    setInputMessage('');
    setIsLoading(true);
    setMessages(prev => [...prev, { role: 'assistant', content: '' }]);

    try {
      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: selectedModel,
          message: userMessage
        })
      });

      if (response.body) {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        while (true) {
          const { value, done } = await reader.read();
          if (done) break;
          const chunk = decoder.decode(value);
          if (chunk) {
            setMessages(prev => {
              const newMessages = [...prev];
              newMessages[newMessages.length - 1].content += chunk;
              return newMessages;
            });
          }
        }
      }
    } catch (error) {
      console.error("Chat error:", error);
      setMessages(prev => {
        const newMessages = [...prev];
        newMessages[newMessages.length - 1].content = "Error connecting to server.";
        return newMessages;
      });
    } finally {
      setIsLoading(false);
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
        
        <div className="settings-section">
          <label>Select Model</label>
          <select 
            value={selectedModel} 
            onChange={e => setSelectedModel(e.target.value)}
            disabled={models.length === 0}
            className="model-select"
          >
            {models.length === 0 ? <option>Loading models...</option> : null}
            {models.map(m => (
              <option key={m} value={m}>{m}</option>
            ))}
          </select>
        </div>

        <button className="settings-button" onClick={openSettings}>
          ⚙️ Settings
        </button>
      </div>
      
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
                    {msg.content}
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
          <button 
            onClick={sendMessage} 
            disabled={isLoading || !inputMessage.trim() || !selectedModel}
            className="send-button"
          >
            {isLoading ? '...' : 'Send'}
          </button>
        </div>
      </div>

      {showSettings && (
        <div className="modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) setShowSettings(false); }}>
          <div className="modal-content">
            <div className="modal-header">
              <h2>Settings</h2>
              <button className="close-button" onClick={() => setShowSettings(false)}>&times;</button>
            </div>
            
            <div className="settings-group">
              <label>Ollama Host Address</label>
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
              <label>Manage Models</label>
              <div className="model-list">
                {settingsModels.length === 0 ? (
                  <div style={{ color: '#8e8ea0', padding: '10px' }}>No models found.</div>
                ) : (
                  settingsModels.map(m => (
                    <div className="model-item" key={m}>
                      <span>{m}</span>
                      <button className="delete-button" onClick={() => deleteModel(m)}>Delete</button>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
