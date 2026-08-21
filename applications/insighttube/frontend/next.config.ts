import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Proxy API calls directly to backend services
  async rewrites() {
    return [
      // Tube Service — direct (bypass Gateway for content APIs)
      {
        source: "/api/v1/content/:path*",
        destination: "http://localhost:8082/api/v1/content/:path*",
      },
      // Tube Service health (via Gateway prefix)
      {
        source: "/tube-service/:path*",
        destination: "http://localhost:8082/tube-service/:path*",
      },
      // Identity via Gateway
      {
        source: "/api/v1/identity/:path*",
        destination: "http://localhost:8080/api/v1/identity/:path*",
      },
      // Identity Service direct (for auth/login)
      {
        source: "/identity-service/:path*",
        destination: "http://localhost:8081/identity-service/:path*",
      },
    ];
  },

  // Allow external image domains (YouTube thumbnails)
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "i.ytimg.com",
      },
    ],
  },
};

export default nextConfig;
