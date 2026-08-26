"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuthStore } from "@/stores/authStore";
import { forgotPassword } from "@/lib/api";
import { Video, Loader2, AlertCircle, Mail, ArrowLeft } from "lucide-react";

type AuthView = "login" | "register" | "forgot-password" | "forgot-sent";

export function LoginPage() {
  const [view, setView] = useState<AuthView>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [username, setUsername] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [forgotEmail, setForgotEmail] = useState("");
  const [localError, setLocalError] = useState("");
  const { login, register, isLoading, error, clearError } = useAuthStore();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError("");
    clearError();
    if (!email || !password) return;
    await login(email, password);
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError("");
    clearError();

    if (!username || !email || !password || !confirmPassword) {
      setLocalError("All fields are required");
      return;
    }
    if (password !== confirmPassword) {
      setLocalError("Passwords do not match");
      return;
    }
    if (password.length < 8) {
      setLocalError("Password must be at least 8 characters");
      return;
    }
    if (!/(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])/.test(password)) {
      setLocalError("Password must contain uppercase, lowercase, number, and special character (@#$%^&+=!)");
      return;
    }
    await register(username, email, password);
  };

  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError("");
    if (!forgotEmail) return;
    try {
      await forgotPassword(forgotEmail, "insighttube");
      setView("forgot-sent");
    } catch {
      setLocalError("If your email is registered, a reset link has been sent.");
    }
  };

  const switchView = (newView: AuthView) => {
    setView(newView);
    setLocalError("");
    clearError();
    // Reset shared form fields to prevent stale values across views
    setEmail("");
    setPassword("");
    setUsername("");
    setConfirmPassword("");
    setForgotEmail("");
  };

  const displayError = localError || error;

  return (
    <div className="flex h-screen items-center justify-center bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950">
      <div className="w-full max-w-sm space-y-6 px-4">
        {/* Logo / Branding */}
        <div className="text-center space-y-2">
          <div className="flex items-center justify-center gap-2 mb-4">
            <div className="rounded-2xl bg-gradient-to-br from-rose-500/20 to-orange-500/20 p-3 border border-rose-500/10">
              <Video className="h-8 w-8 text-rose-400" />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">InsightTube</h1>
          <p className="text-sm text-slate-400">
            Chat with YouTube content using AI
          </p>
        </div>

        {/* Login Form */}
        {view === "login" && (
          <form onSubmit={handleLogin} className="space-y-4">
            <div className="space-y-2">
              <label className="text-xs font-medium text-slate-400">Email</label>
              <Input
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="bg-white/5 border-white/10 text-white placeholder:text-slate-500 focus:border-rose-500/50 focus:ring-rose-500/20"
                autoFocus
                required
              />
            </div>
            <div className="space-y-2">
              <label className="text-xs font-medium text-slate-400">Password</label>
              <Input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="bg-white/5 border-white/10 text-white placeholder:text-slate-500 focus:border-rose-500/50 focus:ring-rose-500/20"
                required
              />
            </div>

            {displayError && (
              <div className="flex items-center gap-2 rounded-lg bg-red-500/10 border border-red-500/20 px-3 py-2.5">
                <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
                <p className="text-xs text-red-400">{displayError}</p>
              </div>
            )}

            <Button
              type="submit"
              disabled={isLoading || !email || !password}
              className="w-full bg-gradient-to-r from-rose-600 to-orange-600 hover:from-rose-500 hover:to-orange-500 text-white shadow-lg shadow-rose-500/20 transition-all"
            >
              {isLoading ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                "Sign In"
              )}
            </Button>

            <div className="flex items-center justify-between text-xs">
              <button
                type="button"
                onClick={() => switchView("forgot-password")}
                className="text-slate-500 hover:text-rose-400 transition-colors"
              >
                Forgot password?
              </button>
              <button
                type="button"
                onClick={() => switchView("register")}
                className="text-slate-500 hover:text-rose-400 transition-colors"
              >
                Create account
              </button>
            </div>
          </form>
        )}

        {/* Register Form */}
        {view === "register" && (
          <form onSubmit={handleRegister} className="space-y-4">
            <div className="flex items-center gap-2 mb-2">
              <button
                type="button"
                onClick={() => switchView("login")}
                className="text-slate-500 hover:text-white transition-colors"
              >
                <ArrowLeft className="h-4 w-4" />
              </button>
              <h2 className="text-sm font-medium text-white">Create your account</h2>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-medium text-slate-400">Username</label>
              <Input
                type="text"
                placeholder="johndoe"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="bg-white/5 border-white/10 text-white placeholder:text-slate-500 focus:border-rose-500/50 focus:ring-rose-500/20"
                autoFocus
                required
                minLength={3}
                maxLength={100}
              />
            </div>
            <div className="space-y-2">
              <label className="text-xs font-medium text-slate-400">Email</label>
              <Input
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="bg-white/5 border-white/10 text-white placeholder:text-slate-500 focus:border-rose-500/50 focus:ring-rose-500/20"
                required
              />
            </div>
            <div className="space-y-2">
              <label className="text-xs font-medium text-slate-400">Password</label>
              <Input
                type="password"
                placeholder="Min 8 chars, upper+lower+number+symbol"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="bg-white/5 border-white/10 text-white placeholder:text-slate-500 focus:border-rose-500/50 focus:ring-rose-500/20"
                required
                minLength={8}
              />
            </div>
            <div className="space-y-2">
              <label className="text-xs font-medium text-slate-400">Confirm Password</label>
              <Input
                type="password"
                placeholder="••••••••"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="bg-white/5 border-white/10 text-white placeholder:text-slate-500 focus:border-rose-500/50 focus:ring-rose-500/20"
                required
              />
            </div>

            {displayError && (
              <div className="flex items-center gap-2 rounded-lg bg-red-500/10 border border-red-500/20 px-3 py-2.5">
                <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
                <p className="text-xs text-red-400">{displayError}</p>
              </div>
            )}

            <Button
              type="submit"
              disabled={isLoading || !username || !email || !password || !confirmPassword}
              className="w-full bg-gradient-to-r from-rose-600 to-orange-600 hover:from-rose-500 hover:to-orange-500 text-white shadow-lg shadow-rose-500/20 transition-all"
            >
              {isLoading ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                "Create Account"
              )}
            </Button>

            <p className="text-xs text-slate-500 text-center">
              Already have an account?{" "}
              <button
                type="button"
                onClick={() => switchView("login")}
                className="text-rose-400 hover:text-rose-300 transition-colors"
              >
                Sign in
              </button>
            </p>
          </form>
        )}

        {/* Forgot Password Form */}
        {view === "forgot-password" && (
          <form onSubmit={handleForgotPassword} className="space-y-4">
            <div className="flex items-center gap-2 mb-2">
              <button
                type="button"
                onClick={() => switchView("login")}
                className="text-slate-500 hover:text-white transition-colors"
              >
                <ArrowLeft className="h-4 w-4" />
              </button>
              <h2 className="text-sm font-medium text-white">Reset your password</h2>
            </div>

            <p className="text-xs text-slate-400">
              Enter your email and we&apos;ll send you a password reset link.
            </p>

            <div className="space-y-2">
              <label className="text-xs font-medium text-slate-400">Email</label>
              <Input
                type="email"
                placeholder="you@example.com"
                value={forgotEmail}
                onChange={(e) => setForgotEmail(e.target.value)}
                className="bg-white/5 border-white/10 text-white placeholder:text-slate-500 focus:border-rose-500/50 focus:ring-rose-500/20"
                autoFocus
                required
              />
            </div>

            {displayError && (
              <div className="flex items-center gap-2 rounded-lg bg-red-500/10 border border-red-500/20 px-3 py-2.5">
                <AlertCircle className="h-4 w-4 text-red-400 shrink-0" />
                <p className="text-xs text-red-400">{displayError}</p>
              </div>
            )}

            <Button
              type="submit"
              disabled={isLoading || !forgotEmail}
              className="w-full bg-gradient-to-r from-rose-600 to-orange-600 hover:from-rose-500 hover:to-orange-500 text-white shadow-lg shadow-rose-500/20 transition-all"
            >
              {isLoading ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                "Send Reset Link"
              )}
            </Button>
          </form>
        )}

        {/* Forgot Password Sent */}
        {view === "forgot-sent" && (
          <div className="space-y-4 text-center">
            <div className="flex items-center justify-center">
              <div className="rounded-full bg-emerald-500/20 p-4 border border-emerald-500/10">
                <Mail className="h-8 w-8 text-emerald-400" />
              </div>
            </div>
            <div className="space-y-2">
              <h2 className="text-lg font-semibold text-white">Check your email</h2>
              <p className="text-sm text-slate-400">
                If an account exists with <span className="text-white">{forgotEmail}</span>,
                we&apos;ve sent a password reset link.
              </p>
            </div>
            <Button
              type="button"
              onClick={() => switchView("login")}
              className="w-full bg-white/10 hover:bg-white/20 text-white border border-white/10"
              variant="ghost"
            >
              Back to Sign In
            </Button>
          </div>
        )}

        {/* Demo credentials hint — only on login view */}
        {view === "login" && (
          <div className="rounded-lg bg-white/5 border border-white/10 px-3 py-2.5">
            <p className="text-xs text-slate-500 text-center">
              Demo: <span className="text-slate-400">demo@insighttube.com</span>{" "}
              / <span className="text-slate-400">Demo@1234</span>
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
