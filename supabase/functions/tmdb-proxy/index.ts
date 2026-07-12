const TMDB_BASE = "https://api.themoviedb.org/3";
const ALLOWED_ORIGIN = Deno.env.get("SUPABASE_URL") || "";

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method === "OPTIONS") {
    return new Response(null, {
      headers: {
        "Access-Control-Allow-Origin": ALLOWED_ORIGIN,
        "Access-Control-Allow-Methods": "GET, OPTIONS",
        "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
      },
    });
  }

  const url = new URL(req.url);
  const path = url.searchParams.get("path");

  if (!path) {
    return new Response(JSON.stringify({ error: "Missing 'path' query parameter" }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    });
  }

  const token = Deno.env.get("TMDB_API_KEY");
  if (!token) {
    return new Response(JSON.stringify({ error: "TMDB_API_KEY not configured" }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }

  const tmdbParams = new URLSearchParams(url.searchParams);
  tmdbParams.delete("path");
  const queryString = tmdbParams.toString();
  const tmdbUrl = `${TMDB_BASE}/${path}${queryString ? "?" + queryString : ""}`;

  try {
    const res = await fetch(tmdbUrl, {
      headers: {
        "Authorization": `Bearer ${token}`,
      },
    });
    const body = await res.text();

    return new Response(body, {
      status: res.status,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": ALLOWED_ORIGIN,
      },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: err.message }), {
      status: 502,
      headers: { "Content-Type": "application/json" },
    });
  }
});
