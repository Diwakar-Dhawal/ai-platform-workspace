"use client";

import { useEffect } from "react";
import { AppLayout } from "@/components/layout/AppLayout";
import { LoginPage } from "@/components/auth/LoginPage";
import { useAuthStore } from "@/stores/authStore";

export default function Home() {
  const { isAuthenticated, loadFromStorage } = useAuthStore();

  useEffect(() => {
    loadFromStorage();
  }, [loadFromStorage]);

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  return <AppLayout />;
}
