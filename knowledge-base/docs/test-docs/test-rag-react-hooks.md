# React Hooks 完全指南

## 1. Hooks 简介

React Hooks 是 React 16.8 引入的新特性，它允许你在不编写 class 的情况下使用 state 和其他 React 特性。

### 1.1 为什么使用 Hooks

- **更简洁的代码**：函数组件比类组件更简洁
- **逻辑复用**：通过自定义 Hooks 复用状态逻辑
- **更好的类型支持**：TypeScript 对函数组件的类型推断更好
- **更容易测试**：函数组件更容易测试

### 1.2 Hooks 规则

1. 只在最顶层使用 Hooks，不要在循环、条件或嵌套函数中调用
2. 只在 React 函数中调用 Hooks

```javascript
// ✅ 正确
function MyComponent() {
  const [count, setCount] = useState(0);
  const [name, setName] = useState('');
  // ...
}

// ❌ 错误
function MyComponent() {
  if (condition) {
    const [count, setCount] = useState(0);  // 条件中调用
  }
}
```

## 2. 基础 Hooks

### 2.1 useState

```javascript
import { useState } from 'react';

function Counter() {
  // 基本用法
  const [count, setCount] = useState(0);

  // 函数式更新（基于前一个状态）
  const increment = () => {
    setCount(prevCount => prevCount + 1);
  };

  // 对象状态
  const [user, setUser] = useState({ name: '', age: 0 });

  const updateName = (newName) => {
    setUser(prevUser => ({
      ...prevUser,
      name: newName
    }));
  };

  // 延迟初始化（性能优化）
  const [data, setData] = useState(() => {
    return expensiveComputation();
  });

  return (
    <div>
      <p>Count: {count}</p>
      <button onClick={() => setCount(count + 1)}>+</button>
      <button onClick={increment}>+ (Functional)</button>
    </div>
  );
}
```

### 2.2 useEffect

```javascript
import { useState, useEffect } from 'react';

function UserProfile({ userId }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // 每次渲染后执行
  useEffect(() => {
    console.log('Component rendered');
  });

  // 只在挂载时执行
  useEffect(() => {
    console.log('Component mounted');
  }, []);

  // 依赖特定状态/属性
  useEffect(() => {
    const fetchUser = async () => {
      setLoading(true);
      try {
        const response = await fetch(`/api/users/${userId}`);
        const data = await response.json();
        setUser(data);
      } catch (error) {
        console.error('Failed to fetch user:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, [userId]); // 只在 userId 变化时执行

  // 清理副作用（订阅、定时器等）
  useEffect(() => {
    const subscription = subscribeToNotifications();

    // 清理函数
    return () => {
      subscription.unsubscribe();
    };
  }, []);

  // 多个 useEffect（关注点分离）
  useEffect(() => {
    document.title = user ? user.name : 'Loading...';
  }, [user]);

  if (loading) return <div>Loading...</div>;
  if (!user) return <div>User not found</div>;

  return <div>{user.name}</div>;
}
```

### 2.3 useContext

```javascript
import { createContext, useContext, useState } from 'react';

// 创建 Context
const ThemeContext = createContext(null);

// Provider 组件
function ThemeProvider({ children }) {
  const [theme, setTheme] = useState('light');

  const toggleTheme = () => {
    setTheme(prev => prev === 'light' ? 'dark' : 'light');
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

// 使用 Context
function ThemedButton() {
  const { theme, toggleTheme } = useContext(ThemeContext);

  return (
    <button
      onClick={toggleTheme}
      style={{
        background: theme === 'light' ? '#fff' : '#333',
        color: theme === 'light' ? '#333' : '#fff'
      }}
    >
      Toggle Theme
    </button>
  );
}

// 自定义 Hook 封装
function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
}

// App 使用
function App() {
  return (
    <ThemeProvider>
      <ThemedButton />
    </ThemeProvider>
  );
}
```

## 3. 额外的 Hooks

### 3.1 useReducer

```javascript
import { useReducer } from 'react';

// 定义 reducer
function todoReducer(state, action) {
  switch (action.type) {
    case 'ADD':
      return [...state, { id: Date.now(), text: action.text, completed: false }];
    case 'TOGGLE':
      return state.map(todo =>
        todo.id === action.id ? { ...todo, completed: !todo.completed } : todo
      );
    case 'DELETE':
      return state.filter(todo => todo.id !== action.id);
    default:
      return state;
  }
}

function TodoList() {
  const [todos, dispatch] = useReducer(todoReducer, []);

  const addTodo = (text) => {
    dispatch({ type: 'ADD', text });
  };

  const toggleTodo = (id) => {
    dispatch({ type: 'TOGGLE', id });
  };

  const deleteTodo = (id) => {
    dispatch({ type: 'DELETE', id });
  };

  return (
    <div>
      {todos.map(todo => (
        <div key={todo.id}>
          <span
            style={{ textDecoration: todo.completed ? 'line-through' : 'none' }}
            onClick={() => toggleTodo(todo.id)}
          >
            {todo.text}
          </span>
          <button onClick={() => deleteTodo(todo.id)}>Delete</button>
        </div>
      ))}
    </div>
  );
}
```

### 3.2 useCallback

```javascript
import { useState, useCallback } from 'react';

function Parent() {
  const [count, setCount] = useState(0);
  const [text, setText] = useState('');

  // 不使用 useCallback - 每次渲染都创建新函数
  const handleClick1 = () => {
    setCount(c => c + 1);
  };

  // 使用 useCallback - 只在依赖变化时创建新函数
  const handleClick2 = useCallback(() => {
    setCount(c => c + 1);
  }, []); // 空依赖数组，函数引用保持不变

  // 带依赖的 useCallback
  const handleSubmit = useCallback(() => {
    console.log(text);
  }, [text]);

  return (
    <div>
      <button onClick={handleClick2}>Count: {count}</button>
      <ChildComponent onClick={handleClick2} />
      <ExpensiveComponent onSubmit={handleSubmit} />
    </div>
  );
}

// 使用 React.memo 避免不必要的重渲染
const ChildComponent = React.memo(function Child({ onClick }) {
  console.log('Child rendered');
  return <button onClick={onClick}>Click me</button>;
});
```

### 3.3 useMemo

```javascript
import { useState, useMemo } from 'react';

function DataList({ data, filter }) {
  const [sortOrder, setSortOrder] = useState('asc');

  // 不使用 useMemo - 每次渲染都重新计算
  const filteredData1 = data.filter(item => item.name.includes(filter));

  // 使用 useMemo - 只在依赖变化时重新计算
  const filteredAndSortedData = useMemo(() => {
    console.log('Computing filtered data...');
    return data
      .filter(item => item.name.includes(filter))
      .sort((a, b) => {
        if (sortOrder === 'asc') {
          return a.name.localeCompare(b.name);
        }
        return b.name.localeCompare(a.name);
      });
  }, [data, filter, sortOrder]);

  // 计算昂贵的值
  const expensiveValue = useMemo(() => {
    return data.reduce((sum, item) => sum + computeExpensiveValue(item), 0);
  }, [data]);

  return (
    <div>
      <p>Total: {expensiveValue}</p>
      {filteredAndSortedData.map(item => (
        <div key={item.id}>{item.name}</div>
      ))}
    </div>
  );
}
```

### 3.4 useRef

```javascript
import { useRef, useEffect } from 'react';

function TextInput() {
  // DOM 引用
  const inputRef = useRef(null);

  // 普通值引用（不触发重渲染）
  const renderCount = useRef(0);
  const previousValue = useRef();

  useEffect(() => {
    renderCount.current++;
    previousValue.current = someValue;
  });

  const focusInput = () => {
    inputRef.current.focus();
  };

  return (
    <div>
      <input ref={inputRef} type="text" />
      <button onClick={focusInput}>Focus</button>
      <p>Render count: {renderCount.current}</p>
    </div>
  );
}

// 保存定时器 ID
function Timer() {
  const intervalRef = useRef(null);

  useEffect(() => {
    intervalRef.current = setInterval(() => {
      console.log('Tick');
    }, 1000);

    return () => {
      clearInterval(intervalRef.current);
    };
  }, []);

  const stopTimer = () => {
    clearInterval(intervalRef.current);
  };

  return <button onClick={stopTimer}>Stop</button>;
}
```

### 3.5 useLayoutEffect

```javascript
import { useLayoutEffect, useRef } from 'react';

function Tooltip() {
  const tooltipRef = useRef(null);
  const [position, setPosition] = useState({ x: 0, y: 0 });

  // useLayoutEffect 在浏览器绘制之前同步执行
  // 用于需要同步执行且影响布局的副作用
  useLayoutEffect(() => {
    const tooltip = tooltipRef.current;
    const { width, height } = tooltip.getBoundingClientRect();

    // 计算位置，确保不超出视口
    setPosition({
      x: Math.min(window.innerWidth - width, targetX),
      y: Math.min(window.innerHeight - height, targetY)
    });
  }, [targetX, targetY]);

  return (
    <div
      ref={tooltipRef}
      style={{ position: 'absolute', left: position.x, top: position.y }}
    >
      Tooltip content
    </div>
  );
}
```

### 3.6 useImperativeHandle

```javascript
import { useRef, useImperativeHandle, forwardRef } from 'react';

// 暴露自定义的 imperative API
const FancyInput = forwardRef(function FancyInput(props, ref) {
  const inputRef = useRef(null);

  useImperativeHandle(ref, () => ({
    focus: () => {
      inputRef.current.focus();
    },
    clear: () => {
      inputRef.current.value = '';
    },
    getValue: () => {
      return inputRef.current.value;
    }
  }), []);

  return <input ref={inputRef} {...props} />;
});

// 使用
function Parent() {
  const fancyInputRef = useRef(null);

  const handleClick = () => {
    fancyInputRef.current.focus();
    console.log(fancyInputRef.current.getValue());
  };

  return (
    <>
      <FancyInput ref={fancyInputRef} />
      <button onClick={handleClick}>Focus and Log</button>
    </>
  );
}
```

## 4. 自定义 Hooks

### 4.1 useLocalStorage

```javascript
import { useState, useEffect } from 'react';

function useLocalStorage(key, initialValue) {
  // 获取初始值
  const [storedValue, setStoredValue] = useState(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch (error) {
      console.error(error);
      return initialValue;
    }
  });

  // 更新 localStorage
  const setValue = (value) => {
    try {
      const valueToStore = value instanceof Function ? value(storedValue) : value;
      setStoredValue(valueToStore);
      window.localStorage.setItem(key, JSON.stringify(valueToStore));
    } catch (error) {
      console.error(error);
    }
  };

  return [storedValue, setValue];
}

// 使用
function App() {
  const [name, setName] = useLocalStorage('name', '');

  return (
    <input
      value={name}
      onChange={e => setName(e.target.value)}
    />
  );
}
```

### 4.2 useFetch

```javascript
import { useState, useEffect } from 'react';

function useFetch(url) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const controller = new AbortController();

    const fetchData = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await fetch(url, {
          signal: controller.signal
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const json = await response.json();
        setData(json);
      } catch (err) {
        if (err.name !== 'AbortError') {
          setError(err.message);
        }
      } finally {
        setLoading(false);
      }
    };

    fetchData();

    // 清理函数
    return () => {
      controller.abort();
    };
  }, [url]);

  return { data, loading, error };
}

// 使用
function UserList() {
  const { data: users, loading, error } = useFetch('/api/users');

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <ul>
      {users.map(user => <li key={user.id}>{user.name}</li>)}
    </ul>
  );
}
```

### 4.3 useDebounce

```javascript
import { useState, useEffect } from 'react';

function useDebounce(value, delay) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(handler);
    };
  }, [value, delay]);

  return debouncedValue;
}

// 使用
function SearchInput() {
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearchTerm = useDebounce(searchTerm, 500);

  useEffect(() => {
    if (debouncedSearchTerm) {
      performSearch(debouncedSearchTerm);
    }
  }, [debouncedSearchTerm]);

  return (
    <input
      type="text"
      value={searchTerm}
      onChange={e => setSearchTerm(e.target.value)}
      placeholder="Search..."
    />
  );
}
```

### 4.4 usePrevious

```javascript
import { useRef, useEffect } from 'react';

function usePrevious(value) {
  const ref = useRef();

  useEffect(() => {
    ref.current = value;
  });

  return ref.current;
}

// 使用
function Counter() {
  const [count, setCount] = useState(0);
  const prevCount = usePrevious(count);

  return (
    <div>
      <p>Now: {count}, Before: {prevCount}</p>
      <button onClick={() => setCount(c => c + 1)}>+</button>
    </div>
  );
}
```

## 5. Hooks 性能优化

### 5.1 使用 React.memo

```javascript
const MyComponent = React.memo(function MyComponent({ data, onUpdate }) {
  // 只在 props 变化时重新渲染
  return <div>{data.name}</div>;
}, (prevProps, nextProps) => {
  // 自定义比较函数
  return prevProps.data.id === nextProps.data.id;
});
```

### 5.2 useMemo 和 useCallback 的最佳实践

```javascript
function OptimizedComponent({ items, onSelect }) {
  // ✅ 缓存昂贵的计算
  const processedItems = useMemo(() => {
    return items.filter(item => item.active)
                .map(item => expensiveTransform(item));
  }, [items]);

  // ✅ 缓存回调函数（传递给子组件时）
  const handleSelect = useCallback((id) => {
    onSelect(id);
  }, [onSelect]);

  // ❌ 不要过度使用
  const simpleValue = useMemo(() => a + b, [a, b]); // 没必要

  return (
    <div>
      {processedItems.map(item => (
        <Item
          key={item.id}
          data={item}
          onSelect={handleSelect}
        />
      ))}
    </div>
  );
}
```

---

本文档全面介绍了 React Hooks 的使用方法，包括基础 Hooks、额外 Hooks、自定义 Hooks 以及性能优化技巧，是 React 函数组件开发的完整参考指南。
