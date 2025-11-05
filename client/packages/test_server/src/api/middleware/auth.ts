import type { Request, Response, NextFunction } from 'express';
import type { ServerConfig } from '../../config/ServerConfig';

export function createAuthMiddleware(config: ServerConfig) {
  return (req: Request, res: Response, next: NextFunction) => {
    // Simple auth - allow all if no auth configured
    if (!config.authUsername || !config.authPassword) {
      return next();
    }

    const auth = req.headers.authorization;
    if (!auth?.startsWith('Basic ')) {
      return res.status(401).json({ error: 'Unauthorized' });
    }

    const [username, password] = Buffer.from(auth.slice(6), 'base64').toString().split(':');
    if (username === config.authUsername && password === config.authPassword) {
      next();
    } else {
      res.status(401).json({ error: 'Invalid credentials' });
    }
  };
}
