"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuthStore } from "@/stores/authStore";
import { Video, Loader2, AlertCircle } from "lucide-react";

export function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const { login, isLoading, error } = useAuthStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) return;
    await login(email, password);
  };

  return (
    <div className="flex h-screen items-center justify-center bg-[#0d0d0d]">
      <div className="w-full max-w-sm space-y-6 px-4">
        {/* Logo / Branding */}
        <div className="text-center space-y-2">
          <div className="flex items-center justify-center gap-2 mb-4">
            <div className="rounded-xl bg-blue-500/20 p-3">
              <Video className="h-8 w-8 text-blue-400" />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-white">InsightTube</h1>
          <p className="text-sm text-white/50">
            Chat with YouTube content using AI
          </p>
        </div>

        {/* Login Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <label className="text-xs font-medium text-white/70">Email</label>
            <Input
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="bg-white/5 border-white/10 text-white placeholder:text-white/30"
              autoFocus
              required
            />
          </div>
          <div className="space-y-2">
            <label className="text-xs font-medium text-white/70">
              Password
            </label>
            <Input
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="bg-white/5 border-white/10 text-white placeholder:text-white/30"
              required
            />
          </div>

          {error && (
            <div className="flex items-center gap-2 rounded-md bg-red-500/10 border border-red-500/20 px-3 py-2">
              <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
              <p className="text-xs text-red-400">{error}</p>
            </div>
          )}

          <Button
            type="submit"
            disabled={isLoading || !email || !password}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white"
          >
            {isLoading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              "Sign In"
            )}
          </Button>
        </form>

        {/* Demo credentials hint */}
        <div className="rounded-md bg-white/5 border border-white/10 px-3 py-2">
          <p className="text-xs text-white/40 text-center">
            Demo: <span className="text-white/60">demo@insighttube.com</span>{" "}
            / <span className="text-white/60">Demo@1234</span>
          </p>
        </div>
      </div>
    </div>
  );
}
