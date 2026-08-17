import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "回声档案馆｜Echo Warrior",
  description: "Echo Warrior 的交互式知识地图与开发百科。",
  icons: {
    icon: "/favicon.png",
    shortcut: "/favicon.png",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
