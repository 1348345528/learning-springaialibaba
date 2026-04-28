import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const useAuthStore = create(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      menus: [],
      permissions: [],
      isAuthenticated: false,

      setAuth: (token, user) => {
        localStorage.setItem('token', token);
        set({ token, user, isAuthenticated: true });
      },

      setUser: (user) => set({ user }),
      setMenus: (menus) => set({ menus }),
      setPermissions: (permissions) => set({ permissions }),

      logout: () => {
        localStorage.removeItem('token');
        set({ token: null, user: null, menus: [], permissions: [], isAuthenticated: false });
        window.location.href = '/login';
      },

      hasPermission: (permission) => {
        const { permissions } = get();
        return permissions.includes(permission);
      },
    }),
    {
      name: 'auth-storage',
      getStorage: () => localStorage,
    }
  )
);

export default useAuthStore;
