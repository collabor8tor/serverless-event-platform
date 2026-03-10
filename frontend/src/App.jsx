import { useEffect, useState } from "react";

const API_URL =
  "https://3kd8eyx2vc.execute-api.us-east-1.amazonaws.com/Prod/todos";

export default function App() {
  const [todos, setTodos] = useState([]);
  const [title, setTitle] = useState("");
  const [loading, setLoading] = useState(false);

  async function loadTodos() {
    setLoading(true);

    try {
      const res = await fetch(API_URL);
      const data = await res.json();
      setTodos(data.items || []);
    } catch (err) {
      console.error(err);
    }

    setLoading(false);
  }

  async function createTodo() {
    if (!title) return;

    try {
      await fetch(API_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ title })
      });

      setTitle("");
      setTimeout(loadTodos, 1000);
    } catch (err) {
      console.error(err);
    }
  }

  useEffect(() => {
    loadTodos();
  }, []);

  return (
    <div style={{ padding: 40, fontFamily: "Arial" }}>
      <h1>AWS Todo Demo</h1>

      <div style={{ marginBottom: 20 }}>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Enter todo"
          style={{ padding: 8, marginRight: 10 }}
        />

        <button onClick={createTodo}>Add Todo</button>

        <button
          style={{ marginLeft: 10 }}
          onClick={loadTodos}
        >
          Refresh
        </button>
      </div>

      {loading && <p>Loading...</p>}

      <ul>
        {todos.map((t) => (
          <li key={t.id}>
            <b>{t.title}</b> — {t.status}
          </li>
        ))}
      </ul>
    </div>
  );
}
