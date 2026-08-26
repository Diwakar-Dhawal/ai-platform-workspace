"use client";

import React, { useEffect, useRef } from "react";
import { WifiOff, Wifi } from "lucide-react";
import { useOnlineStatus } from "@/lib/hooks";

/**
 * Floating banner that appears when the user is offline and disappears when
 * they come back online. Uses a simple state-based approach with toast-like
 * feedback so the user always knows what's happening with connectivity.
 */
export function NetworkStatusBanner() {
  const isOnline = useOnlineStatus();
  const wasOffline = useRef(false);

  // Track transitions: show "reconnected" briefly after coming back online
  const [showReconnected, setShowReconnected] = React.useState(false);

  useEffect(() => {
    if (!isOnline) {
      wasOffline.current = true;
      setShowReconnected(false);
    } else if (wasOffline.current) {
      // Just came back online
      setShowReconnected(true);
      wasOffline.current = false;
      const timer = setTimeout(() => setShowReconnected(false), 3000);
      return () => clearTimeout(timer);
    }
  }, [isOnline]);

  if (!isOnline) {
    return (
      <div className="fixed top-0 left-0 right-0 z-50 bg-red-500/90 backdrop-blur-sm border-b border-red-400/50 px-4 py-2.5 flex items-center justify-center gap-2 animate-in slide-in-from-top">
        <WifiOff className="h-4 w-4 text-white" />
        <span className="text-sm font-medium text-white">
          You&apos;re offline — some features may not work
        </span>
      </div>
    );
  }

  if (showReconnected) {
    return (
      <div className="fixed top-0 left-0 right-0 z-50 bg-emerald-500/90 backdrop-blur-sm border-b border-emerald-400/50 px-4 py-2.5 flex items-center justify-center gap-2 animate-in slide-in-from-top">
        <Wifi className="h-4 w-4 text-white" />
        <span className="text-sm font-medium text-white">
          Back online
        </span>
      </div>
    );
  }

  return null;
}
