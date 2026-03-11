import { useEffect, useState } from "react";

const API_URL =
  import.meta.env.VITE_API_URL ||
  "https://3031zw9gzh.execute-api.us-east-1.amazonaws.com/Prod/todos";

export default function App() {
  const [items, setItems] = useState([]);
  const [title, setTitle] = useState("");
  const [loading, setLoading] = useState(false);
  const [posting, setPosting] = useState(false);
  const [message, setMessage] = useState("");

  async function loadItems() {
    setLoading(true);
    setMessage("");

    try {
      const res = await fetch(API_URL);
      const data = await res.json();
      setItems(data.items || []);
      setMessage("Data loaded successfully.");
    } catch (err) {
      console.error(err);
      setMessage("Failed to load data.");
    }

    setLoading(false);
  }

  async function createItem() {
    if (!title.trim()) return;

    setPosting(true);
    setMessage("");

    try {
      await fetch(API_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ title }),
      });

      setTitle("");
      setMessage("Request submitted. Refreshing data...");
      setTimeout(loadItems, 1200);
    } catch (err) {
      console.error(err);
      setMessage("Failed to submit request.");
    }

    setPosting(false);
  }

  useEffect(() => {
    loadItems();
  }, []);

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <div className="flex min-h-screen">
        {/* Sidebar */}
        <aside className="w-64 bg-slate-900 text-white flex flex-col">
          <div className="px-6 py-5 border-b border-slate-800">
            <h1 className="text-xl font-semibold">Serverless Platform</h1>
            <p className="text-sm text-slate-400 mt-1">AWS Event Dashboard</p>
          </div>

          <nav className="flex-1 px-4 py-6 space-y-2">
            <button className="w-full text-left px-4 py-3 rounded-xl bg-slate-800 hover:bg-slate-700 transition">
              Dashboard
            </button>
            <button className="w-full text-left px-4 py-3 rounded-xl hover:bg-slate-800 transition">
              Requests
            </button>
            <button className="w-full text-left px-4 py-3 rounded-xl hover:bg-slate-800 transition">
              Queue Flow
            </button>
            <button className="w-full text-left px-4 py-3 rounded-xl hover:bg-slate-800 transition">
              Settings
            </button>
          </nav>

          <div className="p-4 border-t border-slate-800 text-sm text-slate-400">
            React + API Gateway + Lambda
          </div>
        </aside>

        {/* Main content */}
        <div className="flex-1 flex flex-col">
          {/* Top nav */}
          <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6">
            <div>
              <h2 className="text-lg font-semibold">Platform Dashboard</h2>
              <p className="text-sm text-slate-500">
                Monitor and submit requests to the serverless backend
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={loadItems}
                className="px-4 py-2 rounded-lg border border-slate-300 bg-white hover:bg-slate-50"
              >
                Refresh
              </button>
              <div className="w-9 h-9 rounded-full bg-blue-600 text-white flex items-center justify-center font-semibold">
                N
              </div>
            </div>
          </header>

          <main className="p-6 space-y-6">
            {/* Stat cards */}
            <section className="grid grid-cols-1 md:grid-cols-3 gap-5">
              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <p className="text-sm text-slate-500">Total Items</p>
                <p className="text-3xl font-bold mt-2">{items.length}</p>
              </div>

              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <p className="text-sm text-slate-500">API Status</p>
                <p className="text-2xl font-semibold mt-2 text-green-600">Healthy</p>
              </div>

              <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-200">
                <p className="text-sm text-slate-500">Last Action</p>
                <p className="text-sm font-medium mt-2">{message || "No recent activity"}</p>
              </div>
            </section>

            {/* Main grid */}
            <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Form card */}
              <div className="bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
                <h3 className="text-lg font-semibold mb-2">Submit Request</h3>
                <p className="text-sm text-slate-500 mb-5">
                  This sends a POST request into your backend workflow.
                </p>

                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium mb-2">Title</label>
                    <input
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                      placeholder="Enter item title"
                      className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>

                  <button
                    onClick={createItem}
                    disabled={posting}
                    className="w-full rounded-xl bg-blue-600 text-white py-3 font-medium hover:bg-blue-700 disabled:opacity-50"
                  >
                    {posting ? "Submitting..." : "Add Item"}
                  </button>
                </div>

                <div className="mt-6 rounded-xl bg-slate-50 border border-slate-200 p-4 text-sm text-slate-600">
                  <p className="font-medium mb-2">Flow</p>
                  <ul className="space-y-1 list-disc list-inside">
                    <li>React UI calls API Gateway</li>
                    <li>Lambda publishes message to SQS</li>
                    <li>Worker Lambda processes queue</li>
                    <li>DynamoDB stores result</li>
                  </ul>
                </div>
              </div>

              {/* Data panel */}
              <div className="lg:col-span-2 bg-white rounded-2xl p-6 shadow-sm border border-slate-200">
                <div className="flex items-center justify-between mb-5">
                  <div>
                    <h3 className="text-lg font-semibold">Stored Items</h3>
                    <p className="text-sm text-slate-500">
                      Loaded through GET from your deployed backend
                    </p>
                  </div>
                  {loading && (
                    <span className="text-sm text-slate-500">Loading...</span>
                  )}
                </div>

                {items.length === 0 ? (
                  <div className="rounded-xl border border-dashed border-slate-300 p-10 text-center text-slate-500">
                    No items found yet.
                  </div>
                ) : (
                  <div className="overflow-hidden rounded-xl border border-slate-200">
                    <table className="min-w-full text-sm">
                      <thead className="bg-slate-50">
                        <tr>
                          <th className="text-left px-4 py-3 font-medium text-slate-600">ID</th>
                          <th className="text-left px-4 py-3 font-medium text-slate-600">Title</th>
                          <th className="text-left px-4 py-3 font-medium text-slate-600">Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {items.map((item, index) => (
                          <tr
                            key={item.id || index}
                            className="border-t border-slate-200"
                          >
                            <td className="px-4 py-3 text-slate-500">{item.id}</td>
                            <td className="px-4 py-3 font-medium">{item.title}</td>
                            <td className="px-4 py-3">
                              <span className="inline-flex rounded-full bg-emerald-100 text-emerald-700 px-3 py-1 text-xs font-semibold">
                                {item.status}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </section>
          </main>
        </div>
      </div>
    </div>
  );
}
