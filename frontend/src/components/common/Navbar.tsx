import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Bell, LogOut, User as UserIcon, ShieldAlert, BookOpen, GraduationCap } from 'lucide-react';
import { apiClient } from '../../api/client';
import { NotificationItem } from '../../types';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [showNotifications, setShowNotifications] = useState<boolean>(false);

  useEffect(() => {
    if (user) {
      loadNotifications();
    }
  }, [user]);

  const loadNotifications = async () => {
    try {
      const res = await apiClient.get<NotificationItem[]>('/notifications');
      setNotifications(res.data.slice(0, 5));
      const unreadRes = await apiClient.get<{ unreadCount: number }>('/notifications/unread-count');
      setUnreadCount(unreadRes.data.unreadCount || 0);
    } catch (err) {
      // Ignore background notification fetch errors
    }
  };

  const markRead = async (id: number) => {
    try {
      await apiClient.patch(`/notifications/${id}/read`);
      setUnreadCount((prev) => Math.max(0, prev - 1));
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
      );
    } catch (err) {
      // ignore
    }
  };

  const getRoleBadge = () => {
    if (!user) return null;
    switch (user.role) {
      case 'MANAGEMENT':
        return (
          <span className="flex items-center gap-1 text-xs px-2.5 py-1 bg-purple-100 text-purple-800 font-semibold rounded-full border border-purple-200">
            <ShieldAlert className="w-3.5 h-3.5" /> Management
          </span>
        );
      case 'TEACHER':
        return (
          <span className="flex items-center gap-1 text-xs px-2.5 py-1 bg-blue-100 text-blue-800 font-semibold rounded-full border border-blue-200">
            <BookOpen className="w-3.5 h-3.5" /> Faculty
          </span>
        );
      case 'STUDENT':
        return (
          <span className="flex items-center gap-1 text-xs px-2.5 py-1 bg-emerald-100 text-emerald-800 font-semibold rounded-full border border-emerald-200">
            <GraduationCap className="w-3.5 h-3.5" /> Student
          </span>
        );
    }
  };

  return (
    <header className="sticky top-0 z-40 bg-white border-b border-slate-200 shadow-xs">
      <div className="px-4 sm:px-6 lg:px-8 flex items-center justify-between h-16">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-linear-to-tr from-blue-700 to-indigo-600 flex items-center justify-center text-white font-extrabold shadow-sm tracking-wider">
            EF
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
              ExamForge
              <span className="text-[10px] uppercase font-bold tracking-widest px-1.5 py-0.5 bg-blue-50 text-blue-700 rounded border border-blue-200">
                AI Powered
              </span>
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-4">
          {getRoleBadge()}

          {/* Notifications Dropdown */}
          <div className="relative">
            <button
              onClick={() => setShowNotifications(!showNotifications)}
              className="relative p-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-lg transition-colors"
              title="Notifications"
            >
              <Bell className="w-5 h-5" />
              {unreadCount > 0 && (
                <span className="absolute top-1.5 right-1.5 w-4 h-4 bg-rose-600 text-white text-[10px] font-bold rounded-full flex items-center justify-center">
                  {unreadCount}
                </span>
              )}
            </button>

            {showNotifications && (
              <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-slate-200 py-2 z-50 animate-in fade-in zoom-in-95">
                <div className="px-4 py-2 border-b border-slate-100 flex items-center justify-between">
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                    Notifications
                  </h4>
                  <span className="text-xs text-blue-600 font-medium">
                    {unreadCount} unread
                  </span>
                </div>
                <div className="max-h-64 overflow-y-auto divide-y divide-slate-100">
                  {notifications.length === 0 ? (
                    <p className="p-4 text-xs text-slate-400 text-center">No notifications yet</p>
                  ) : (
                    notifications.map((n) => (
                      <div
                        key={n.id}
                        onClick={() => markRead(n.id)}
                        className={`p-3 text-xs hover:bg-slate-50 cursor-pointer ${
                          !n.isRead ? 'bg-blue-50/40' : ''
                        }`}
                      >
                        <p className="font-semibold text-slate-800">{n.title}</p>
                        <p className="text-slate-600 mt-0.5 line-clamp-2">{n.message}</p>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="h-6 w-[1px] bg-slate-200" />

          {/* User info & Logout */}
          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <p className="text-sm font-semibold text-slate-800">{user?.fullName}</p>
              <p className="text-xs text-slate-500">{user?.email}</p>
            </div>
            <button
              onClick={logout}
              className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors flex items-center gap-1.5 text-xs font-semibold"
              title="Logout"
            >
              <LogOut className="w-4 h-4" />
              <span className="hidden md:inline">Logout</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
