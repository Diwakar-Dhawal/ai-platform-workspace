import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Proxy API calls to the Gateway service
  async rewrites() {
    return [
      {
        source: "/tube-service/:path*",
        destination: "http://localhost:8080/tube-service/:path*",
      },
      {
        source: "/identity/:path*",
        destination: "http://localhost:8080/identity/:path*",
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
