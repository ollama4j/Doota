import { useState, useEffect, useRef, useCallback } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { ConfirmDialog } from './ConfirmDialog';
import { Copy, Check, Astroid, User } from 'lucide-react';
import './App.css';

const MAX_AGENT_ITERATIONS = 10;

interface ChatMessage {
  role: 'user' | 'assistant' | 'tool';
  content: string;
  tps?: number;
  tool_calls?: any[];
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

interface OllamaModel {
  name: string;
  modifiedAt?: string;
  modified_at?: string;
  size?: number;
}

interface ToolCallAccordionCardProps {
  tc: any;
}

function ToolCallAccordionCard({ tc }: ToolCallAccordionCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);

  const toolName = tc.function?.name ?? tc.name ?? 'unknown';
  const toolArgs = tc.function?.arguments ?? tc.arguments ?? {};
  const status = tc.status ?? 'pending';
  const result = tc.result;

  let statusIcon = '⏳';
  let statusText: React.ReactNode = <span>invoking tool: <code>{toolName}</code></span>;
  let statusClass = 'pending';

  if (status === 'running') {
    statusIcon = '⚙️';
    statusText = <span>Invoking tool <code>{toolName}</code></span>;
    statusClass = 'running';
  } else if (status === 'success') {
    statusIcon = '✅';
    statusText = <span>Tool <code>{toolName}</code> execution complete</span>;
    statusClass = 'success';
  } else if (status === 'error') {
    statusIcon = '❌';
    statusText = <span>Tool <code>{toolName}</code> failed</span>;
    statusClass = 'error';
  }

  return (
    <div className={`tool-call-accordion-card ${statusClass}`}>
      <div
        className="tool-call-accordion-header"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <span className={`tool-call-accordion-icon ${statusClass === 'running' ? 'spin' : ''}`}>
          {statusIcon}
        </span>
        <span className="tool-call-accordion-title">{statusText}</span>
        <span className="tool-call-accordion-arrow">{isExpanded ? '▲' : '▼'}</span>
      </div>

      {isExpanded && (
        <div className="tool-call-accordion-body">
          <div className="tool-call-section">
            <div className="tool-call-section-title">Parameters</div>
            <pre className="tool-call-code">{JSON.stringify(toolArgs, null, 2)}</pre>
          </div>

          {(status === 'running' || status === 'pending') && (
            <div className="tool-call-section">
              <div className="tool-call-section-title">Response</div>
              <div className="tool-call-loading">
                <span className="tool-call-loading-dot">.</span>
                <span className="tool-call-loading-dot">.</span>
                <span className="tool-call-loading-dot">.</span>
              </div>
            </div>
          )}

          {result && (
            <div className="tool-call-section">
              <div className="tool-call-section-title">Response</div>
              <pre className="tool-call-code">{result}</pre>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function App() {
  const [models, setModels] = useState<string[]>([]);
  const [tools, setTools] = useState<ToolInfoDTO[]>([]);
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [isConversationsLoaded, setIsConversationsLoaded] = useState(false);
  const conversationsRef = useRef<Conversation[]>([]);

  useEffect(() => {
    conversationsRef.current = conversations;
  }, [conversations]);

  const [currentConversationId, setCurrentConversationId] = useState<string | null>(null);
  const [currentPath, setCurrentPath] = useState(window.location.pathname);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [agentStatus, setAgentStatus] = useState<string>('');
  const [agentIteration, setAgentIteration] = useState(0);
  const [hostUrl, setHostUrl] = useState('');
  const [settingsModels, setSettingsModels] = useState<OllamaModel[]>([]);
  const [loadedModels, setLoadedModels] = useState<string[]>([]);
  const [modelDetails, setModelDetails] = useState<Record<string, any>>({});
  const [isServerReachable, setIsServerReachable] = useState<boolean | null>(null);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [chatToDelete, setChatToDelete] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);
  const [modelSearch, setModelSearch] = useState('');
  const [toolsSearch, setToolsSearch] = useState('');
  const [manageModelsSearch, setManageModelsSearch] = useState('');
  const [modelFamilyFilter, setModelFamilyFilter] = useState('all');
  const [modelSortOrder, setModelSortOrder] = useState('name-asc');
  const [isModelDropdownOpen, setIsModelDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const filteredModels = models.filter(m =>
    m.toLowerCase().includes(modelSearch.toLowerCase())
  );

  const filteredTools = tools.filter(t =>
    (t.displayName || t.name || '').toLowerCase().includes(toolsSearch.toLowerCase()) ||
    (t.description || '').toLowerCase().includes(toolsSearch.toLowerCase())
  );

  // Extract unique model families dynamically from loaded model details
  const uniqueFamilies = Array.from(
    new Set(
      settingsModels
        .map(m => modelDetails[m.name]?.details?.family)
        .filter(Boolean) as string[]
    )
  ).sort();

  const processedSettingsModels = settingsModels
    .filter(m => {
      const query = manageModelsSearch.toLowerCase().trim();
      if (!query) return true;

      const details = modelDetails[m.name]?.details || {};
      const family = (details.family || '').toLowerCase();
      const params = (details.parameter_size || '').toLowerCase();
      const quantization = (details.quantization_level || '').toLowerCase();
      const name = m.name.toLowerCase();

      return (
        name.includes(query) ||
        family.includes(query) ||
        params.includes(query) ||
        quantization.includes(query)
      );
    })
    .filter(m => {
      if (modelFamilyFilter === 'all') return true;
      const family = modelDetails[m.name]?.details?.family;
      return family === modelFamilyFilter;
    })
    .sort((a, b) => {
      if (modelSortOrder === 'name-asc') {
        return a.name.localeCompare(b.name);
      }
      if (modelSortOrder === 'name-desc') {
        return b.name.localeCompare(a.name);
      }
      if (modelSortOrder === 'size-asc') {
        return (a.size || 0) - (b.size || 0);
      }
      if (modelSortOrder === 'size-desc') {
        return (b.size || 0) - (a.size || 0);
      }
      if (modelSortOrder === 'date-asc') {
        const dateA = new Date(a.modifiedAt || a.modified_at || 0).getTime();
        const dateB = new Date(b.modifiedAt || b.modified_at || 0).getTime();
        return dateA - dateB;
      }
      if (modelSortOrder === 'date-desc') {
        const dateA = new Date(a.modifiedAt || a.modified_at || 0).getTime();
        const dateB = new Date(b.modifiedAt || b.modified_at || 0).getTime();
        return dateB - dateA;
      }
      return 0;
    });

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

  // ─── Server-side conversation persistence helpers ─────────────────────────

  const saveConversationToServer = (conv: Conversation) => {
    fetch(`/api/chats/${conv.id}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(conv)
    }).catch(err => console.error('Failed to save conversation', err));
  };

  const deleteConversationFromServer = (id: string) => {
    fetch(`/api/chats/${id}`, { method: 'DELETE' })
      .catch(err => console.error('Failed to delete conversation', err));
  };

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
        if (data && Array.isArray(data)) {
          const isObjectArray = data.length > 0 && typeof data[0] === 'object';
          const modelNames = isObjectArray ? data.map((m: any) => m.name) : data;
          const modelObjects = isObjectArray ? data : data.map((m: string) => ({ name: m }));

          setModels(modelNames);
          setSettingsModels(modelObjects);

          if (modelNames.length > 0) {
            modelNames.forEach((m: string) => fetchModelDetails(m));
            if (currentConversation && currentConversation.model) {
              setSelectedModel(currentConversation.model);
            } else {
              setSelectedModel(modelNames[0]);
            }
          } else {
            setSelectedModel('');
          }
        } else {
          setModels([]);
          setSettingsModels([]);
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

  // Load conversations from server on mount
  useEffect(() => {
    fetch('/api/chats')
      .then(res => {
        if (!res.ok) throw new Error('Failed to load chats');
        return res.json();
      })
      .then((serverData: Conversation[]) => {
        // Merge: server is source of truth; keep any local-only convs
        // not yet persisted (e.g. a brand-new chat created before server responded)
        setConversations(prev => {
          const serverMap = new Map(serverData.map(c => [c.id, c]));
          const localOnly = prev.filter(c => !serverMap.has(c.id));
          return [...serverData, ...localOnly];
        });
        setIsConversationsLoaded(true);
      })
      .catch(err => {
        console.error('Failed to load conversations from server', err);
        setIsConversationsLoaded(true); // fall back to local flow even on error
      });
  }, []);

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
    if (!isConversationsLoaded) return;

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
      const existing = conversationsRef.current.find(c => c.id === uuid);
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
        setConversations(prev => {
          if (prev.some(c => c.id === uuid)) return prev;
          return [newConv, ...prev];
        });
        setCurrentConversationId(uuid);
        saveConversationToServer(newConv);
      }
    } else {
      if (conversationsRef.current.length > 0) {
        navigateTo(`/chat/${conversationsRef.current[0].id}`);
      } else {
        const newUuid = crypto.randomUUID();
        navigateTo(`/chat/${newUuid}`);
      }
    }
  }, [currentPath, isConversationsLoaded]);

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
    deleteConversationFromServer(id);

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
    setAgentStatus('');
    setAgentIteration(0);
  };

  // ─── UI-side agentic loop ────────────────────────────────────────────────
  // All LLM calls, tool detection, and tool execution happen here in the
  // browser. The server only streams LLM tokens and executes individual tools.

  const runAgentLoop = useCallback(async (
    initialHistory: ChatMessage[],
    conversationId: string,
    model: string
  ) => {
    setIsLoading(true);
    setAgentIteration(0);
    setAgentStatus('Thinking…');

    // Working copy of messages we mutate across iterations.
    // The last element is always the current (streaming) assistant bubble.
    let workingMessages = [...initialHistory];

    let latestConvSnapshot: Conversation | null = null;

    const updateConv = (msgs: ChatMessage[]) => {
      if (latestConvSnapshot) {
        latestConvSnapshot = { ...latestConvSnapshot, messages: msgs };
      } else {
        const currentConv = conversationsRef.current.find(c => c.id === conversationId);
        if (currentConv) {
          latestConvSnapshot = { ...currentConv, messages: msgs };
        }
      }
      setConversations(prev => prev.map(c => {
        if (c.id === conversationId) {
          return { ...c, messages: msgs };
        }
        return c;
      }));
    };

    try {
      for (let iteration = 0; iteration < MAX_AGENT_ITERATIONS; iteration++) {
        setAgentIteration(iteration + 1);
        setAgentStatus(`Asking the model...`);

        // Build message list for the API (exclude the trailing empty assistant bubble)
        const historyForApi = workingMessages
          .slice(0, -1)  // drop the placeholder assistant message
          .map(m => ({ role: m.role, content: m.content, tool_calls: m.tool_calls }));

        // ── 1. Stream LLM response ──────────────────────────────────────────
        const res = await fetch('/api/chat/stream', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          signal: abortControllerRef.current?.signal ?? null,
          body: JSON.stringify({ model, messages: historyForApi })
        });

        if (!res.ok) {
          let errorMsg = 'The model returned ' + res.status;
          try {
            const errorJson = await res.json();
            if (errorJson && typeof errorJson === 'object') {
              const details = errorJson.details || errorJson.message || '';
              if (details.includes('does not support tools')) {
                errorMsg = 'The model does not support tools. Try a different model.';
              }
            }
          } catch (e) {
            // ignore and fallback
          }
          throw new Error(errorMsg);
        }
        if (!res.body) throw new Error('No response body');

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        let assistantText = '';
        let detectedToolCalls: any[] = [];

        outer: while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });

          // Lines are newline-delimited JSON objects
          const lines = buffer.split('\n');
          buffer = lines.pop() ?? '';

          for (const line of lines) {
            const trimmed = line.trim();
            if (!trimmed) continue;
            try {
              const event = JSON.parse(trimmed);
              if (event.type === 'text') {
                assistantText += event.content;
                // Live-update the streaming assistant bubble
                workingMessages = [
                  ...workingMessages.slice(0, -1),
                  { role: 'assistant', content: assistantText, tps: event.tps }
                ];
                updateConv(workingMessages);
              } else if (event.type === 'tool_call') {
                detectedToolCalls = event.toolCalls ?? [];
                // Attach tool_calls to the current assistant message with status 'pending'
                const toolCallsWithStatus = detectedToolCalls.map(tc => ({
                  ...tc,
                  status: 'pending'
                }));
                workingMessages = [
                  ...workingMessages.slice(0, -1),
                  { role: 'assistant', content: assistantText, tool_calls: toolCallsWithStatus }
                ];
                updateConv(workingMessages);
                break outer;
              } else if (event.type === 'done') {
                const streamTps = event.tps;
                workingMessages = [
                  ...workingMessages.slice(0, -1),
                  { role: 'assistant', content: assistantText, tps: streamTps }
                ];
                updateConv(workingMessages);
              }
            } catch { /* ignore malformed lines */ }
          }
        }

        // ── 2. No tool call → final answer, done ───────────────────────────
        if (detectedToolCalls.length === 0) {
          setAgentStatus('');
          break;
        }

        // ── 3. Execute each tool and append results ─────────────────────────
        setAgentStatus(`Iteration ${iteration + 1} — executing ${detectedToolCalls.length} tool(s)…`);

        for (const tc of detectedToolCalls) {
          const toolName = tc.function?.name ?? tc.name;
          const toolArgs = tc.function?.arguments ?? tc.arguments ?? {};

          // Update the assistant message's tool call status to 'running'
          workingMessages = workingMessages.map(m => {
            if (m.role === 'assistant' && m.tool_calls) {
              const updatedCalls = m.tool_calls.map(call => {
                if ((call.function?.name ?? call.name) === toolName) {
                  return { ...call, status: 'running' };
                }
                return call;
              });
              return { ...m, tool_calls: updatedCalls };
            }
            return m;
          });
          updateConv(workingMessages);

          setAgentStatus(`invoking tool: ${toolName}`);

          let resultContent: string;
          let toolSuccess = true;
          try {
            const toolRes = await fetch('/api/tools/execute', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              signal: abortControllerRef.current?.signal ?? null,
              body: JSON.stringify({ toolName, arguments: toolArgs })
            });
            const textResponse = await toolRes.text();
            let data;
            try {
              data = JSON.parse(textResponse);
            } catch (e) {
              data = textResponse;
            }
            resultContent = typeof data === 'object' ? JSON.stringify(data, null, 2) : String(data);
          } catch (err: any) {
            toolSuccess = false;
            resultContent = `Error executing tool "${toolName}": ${err?.message ?? err}`;
          }

          // Append tool result message for API history
          workingMessages = [
            ...workingMessages,
            { role: 'tool', content: resultContent }
          ];

          // Update the assistant message's tool call with result and status
          workingMessages = workingMessages.map(m => {
            if (m.role === 'assistant' && m.tool_calls) {
              const updatedCalls = m.tool_calls.map(call => {
                if ((call.function?.name ?? call.name) === toolName) {
                  return {
                    ...call,
                    status: toolSuccess ? 'success' : 'error',
                    result: resultContent
                  };
                }
                return call;
              });
              return { ...m, tool_calls: updatedCalls };
            }
            return m;
          });
          updateConv(workingMessages);
        }

        // ── 4. Add empty assistant bubble for the next iteration ────────────
        workingMessages = [
          ...workingMessages,
          { role: 'assistant', content: '' }
        ];
        updateConv(workingMessages);

        setAgentStatus(`Iteration ${iteration + 1} complete — continuing…`);
      }
    } catch (error: any) {
      if (error?.name === 'AbortError') {
        // User stopped generation — leave whatever was streamed
        setAgentStatus('');
      } else {
        console.error('Agent loop error:', error);
        // Append error to the trailing assistant bubble
        workingMessages = [
          ...workingMessages.slice(0, -1),
          {
            role: 'assistant',
            content: (workingMessages.at(-1)?.content ?? '') +
              '\n\n⚠️ Error: ' + (error?.message ?? String(error))
          }
        ];
        updateConv(workingMessages);
      }
    } finally {
      setIsLoading(false);
      setAgentStatus('');
      setAgentIteration(0);
      // Persist the final state of the conversation to the server
      if (latestConvSnapshot) {
        saveConversationToServer(latestConvSnapshot);

        const snapshot = latestConvSnapshot as Conversation;
        const userMsgs = snapshot.messages.filter(m => m.role === 'user');
        const firstUserMsg = userMsgs[0];

        const isDefaultOrTempTitle =
          snapshot.title === 'New Chat' ||
          (firstUserMsg && (
            snapshot.title === firstUserMsg.content ||
            snapshot.title === (firstUserMsg.content.length > 35 ? firstUserMsg.content.substring(0, 35) + '...' : firstUserMsg.content)
          ));

        if (isDefaultOrTempTitle && userMsgs.length === 1 && snapshot.messages.length >= 2) {
          if (firstUserMsg && firstUserMsg.content) {
            fetch(`/api/chats/${conversationId}/generate-title`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                model: snapshot.model,
                firstMessage: firstUserMsg.content
              })
            })
              .then(res => {
                if (!res.ok) throw new Error('Title generation failed');
                return res.json();
              })
              .then(data => {
                if (data.title) {
                  setConversations(prev => prev.map(c => {
                    if (c.id === conversationId) {
                      return { ...c, title: data.title };
                    }
                    return c;
                  }));
                }
              })
              .catch(err => console.error('Failed to generate title', err));
          }
        }
      }
    }
  }, []);

  const sendMessage = () => {
    if (!inputMessage.trim() || !selectedModel || !currentConversationId) return;

    const userMessage = inputMessage;
    setInputMessage('');

    abortControllerRef.current = new AbortController();

    let currentHistory: ChatMessage[] = [];
    let snapshotConv: Conversation | null = null;
    const convId = currentConversationId;
    const model = selectedModel;

    setConversations(prev => prev.map(c => {
      if (c.id === convId) {
        const updatedMessages: ChatMessage[] = [
          ...c.messages,
          { role: 'user', content: userMessage },
          { role: 'assistant', content: '' }  // placeholder bubble for streaming
        ];
        const title = c.title === 'New Chat'
          ? (userMessage.length > 35 ? userMessage.substring(0, 35) + '...' : userMessage)
          : c.title;
        currentHistory = updatedMessages;
        snapshotConv = { ...c, messages: updatedMessages, title, model };
        return snapshotConv;
      }
      return c;
    }));

    // Persist user message immediately so it isn't lost on refresh
    if (snapshotConv) {
      saveConversationToServer(snapshotConv);
    }

    setTimeout(() => {
      runAgentLoop(currentHistory, convId, model);
    }, 0);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      sendMessage();
    }
  };

  return (
    <div className="app-container">
      <div className="sidebar">
        <div
          className="logo-container"
          title="Doota (written as dūta or ದೂತ/ದೂತ in Indian languages) primarily translates to messenger, envoy, ambassador, or emissary."
        >
          <div className="app-logo-wrapper">
            <img src="/logo.png" alt="Doota Logo" className="app-logo" />
          </div>
          <div className="nav-logo-text">
            <span>Doota</span>
          </div>
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
              <div className="tools-search-container" style={{ marginBottom: '15px' }}>
                <input
                  type="text"
                  placeholder="Search tools by name or description..."
                  value={toolsSearch}
                  onChange={e => setToolsSearch(e.target.value)}
                  className="tools-search-input"
                />
              </div>
              <div className="tools-list">
                {filteredTools.length === 0 ? (
                  <div style={{ color: '#8e8ea0', padding: '10px' }}>
                    {tools.length === 0 ? "No tools registered." : "No tools match your search criteria."}
                  </div>
                ) : (
                  filteredTools.map(t => (
                    <div className="tool-item" key={t.name}>
                      <div className="tool-info-container">
                        <div className="tool-name-row">
                          <span className="tool-name">{t.displayName}</span>
                          <button
                            className={`toggle-tool-button ${t.enabled ? 'enabled' : 'disabled'}`}
                            onClick={() => toggleTool(t.name)}
                          >
                            {t.enabled ? 'Disable' : 'Enable'}
                          </button>
                        </div>
                        <p className="tool-description">{t.description}</p>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="settings-group">
              <label>Manage Models</label>
              <div style={{ color: '#8e8ea0', fontSize: '0.85rem', marginBottom: '15px', lineHeight: '1.4' }}>
                View, filter, sort, and delete local Ollama models currently downloaded on your host.
              </div>

              {/* Models Search & Filter Controls */}
              <div className="models-controls-panel">
                <div className="models-search-wrapper">
                  <input
                    type="text"
                    placeholder="Search models by name, family, parameter size..."
                    value={manageModelsSearch}
                    onChange={e => setManageModelsSearch(e.target.value)}
                    className="models-search-input"
                  />
                </div>
                <div className="models-filters-wrapper">
                  <select
                    value={modelFamilyFilter}
                    onChange={e => setModelFamilyFilter(e.target.value)}
                    className="models-select-filter"
                  >
                    <option value="all">All Families</option>
                    {uniqueFamilies.map(fam => (
                      <option key={fam} value={fam}>{fam.toUpperCase()}</option>
                    ))}
                  </select>

                  <select
                    value={modelSortOrder}
                    onChange={e => setModelSortOrder(e.target.value)}
                    className="models-select-filter"
                  >
                    <option value="name-asc">Sort: Name (A-Z)</option>
                    <option value="name-desc">Sort: Name (Z-A)</option>
                    <option value="size-asc">Sort: Size (Smallest)</option>
                    <option value="size-desc">Sort: Size (Largest)</option>
                    <option value="date-desc">Sort: Recently Modified</option>
                    <option value="date-asc">Sort: Oldest Modified</option>
                  </select>
                </div>
              </div>

              <div className="models-grid">
                {processedSettingsModels.length === 0 ? (
                  <div style={{ color: '#8e8ea0', padding: '10px 10px 20px', gridColumn: '1 / -1', textAlign: 'center' }}>
                    {settingsModels.length === 0 ? "No models found." : "No models match your search or filter criteria."}
                  </div>
                ) : (
                  processedSettingsModels.map(m => {
                    const modelName = m.name;
                    const dateStr = m.modifiedAt || m.modified_at;
                    const sizeBytes = m.size;

                    let dateDisplay = '';
                    if (dateStr) {
                      try {
                        const d = new Date(dateStr);
                        dateDisplay = d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                      } catch (e) {
                        dateDisplay = dateStr;
                      }
                    }

                    let sizeDisplay = '';
                    if (sizeBytes) {
                      if (sizeBytes > 1024 * 1024 * 1024) {
                        sizeDisplay = (sizeBytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
                      } else {
                        sizeDisplay = (sizeBytes / (1024 * 1024)).toFixed(2) + ' MB';
                      }
                    }

                    return (
                      <div className="model-card" key={modelName}>
                        <div className="model-card-info">
                          <div className="model-card-header-row">
                            <span className="model-card-name" title={modelName}>{modelName}</span>
                            <button className="model-card-delete-btn" onClick={() => deleteModel(modelName)}>Delete</button>
                          </div>
                          <div className="model-card-meta">
                            {sizeDisplay && <span className="model-card-meta-item">📦 {sizeDisplay}</span>}
                            {dateDisplay && <span className="model-card-meta-item">🕒 {dateDisplay}</span>}
                          </div>
                          {modelDetails[modelName] && (
                            <div className="model-details-badge-container">
                              {modelDetails[modelName].details?.family && (
                                <span className="model-badge family">Family: {modelDetails[modelName].details.family}</span>
                              )}
                              {modelDetails[modelName].details?.parameter_size && (
                                <span className="model-badge params">Params: {modelDetails[modelName].details.parameter_size}</span>
                              )}
                              {modelDetails[modelName].details?.quantization_level && (
                                <span className="model-badge quantization">Quant: {modelDetails[modelName].details.quantization_level}</span>
                              )}
                              {modelDetails[modelName].details?.format && (
                                <span className="model-badge format">Format: {modelDetails[modelName].details.format}</span>
                              )}
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })
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
              messages.map((msg, i) => {
                if (msg.role === 'tool') return null;
                return (
                  <div key={i} className={`message-row ${msg.role}`}>
                    <div className="message-container">
                      <div className={`avatar ${msg.role}`}>
                        {msg.role === 'user' ? <User size={20} /> : <Astroid size={20} />}
                      </div>
                      <div className="message-content">
                        <div className={`message-bubble ${msg.role}`}>
                          <ReactMarkdown remarkPlugins={[remarkGfm]}>
                            {msg.content}
                          </ReactMarkdown>

                          {msg.tool_calls && msg.tool_calls.length > 0 && (
                            <div className="tool-calls-container">
                              {msg.tool_calls.map((tc, tcIndex) => (
                                <ToolCallAccordionCard key={tcIndex} tc={tc} />
                              ))}
                            </div>
                          )}
                        </div>
                        <div className="message-actions">
                          {msg.role === 'assistant' && msg.tps != null && (
                            <span className="tps-indicator" title="Average tokens generated per second">
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
                  </div>
                );
              })
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Agent status bar – visible while the loop is running */}
          {isLoading && agentStatus && (
            <div className="agent-status-bar">
              <span className="agent-status-spinner">⟳</span>
              <span className="agent-status-text">{agentStatus}</span>
              {agentIteration > 0 && (
                <span className="agent-iteration-badge">
                  Iteration {agentIteration} / {MAX_AGENT_ITERATIONS}
                </span>
              )}
            </div>
          )}

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
