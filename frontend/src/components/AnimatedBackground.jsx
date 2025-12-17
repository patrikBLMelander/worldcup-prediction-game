import { useEffect, useRef } from 'react';
import './AnimatedBackground.css';

const AnimatedBackground = () => {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    let animationFrameId;
    let particles = [];

    // Set canvas size
    const resizeCanvas = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);

    // Regular Particle class
    class Particle {
      constructor() {
        this.reset();
        this.y = Math.random() * canvas.height;
        this.isFastStar = false;
      }

      reset() {
        this.x = Math.random() * canvas.width;
        this.y = Math.random() * canvas.height;
        // Slightly smaller, finer pixels
        this.size = Math.random() * 0.8 + 0.7; // ~0.7 - 1.5px
        this.speedX = (Math.random() - 0.5) * 0.2; // gentle drift horizontally
        this.speedY = Math.random() * 0.25 + 0.08; // slow downward movement
        this.opacity = Math.random() * 0.4 + 0.4; // 0.4 - 0.8
        this.trail = [];
        this.maxTrailLength = 5;
        this.isFastStar = false;
      }

      update() {
        // Add current position to trail
        this.trail.push({ x: this.x, y: this.y, opacity: this.opacity });
        if (this.trail.length > this.maxTrailLength) {
          this.trail.shift();
        }

        this.x += this.speedX;
        this.y += this.speedY;

        // Reset particle if it goes off screen
        if (this.y > canvas.height) {
          this.reset();
          this.y = -10;
        }
        if (this.x < 0 || this.x > canvas.width) {
          this.x = Math.random() * canvas.width;
        }
      }

      draw() {
        // Draw trail
        for (let i = 0; i < this.trail.length; i++) {
          const point = this.trail[i];
          const trailOpacity = (point.opacity * (i + 1)) / this.trail.length * 0.3;
          ctx.beginPath();
          ctx.arc(point.x, point.y, this.size * 0.5, 0, Math.PI * 2);
          ctx.fillStyle = `rgba(129, 230, 217, ${trailOpacity})`; // teal trail for contrast
          ctx.fill();
        }

        // Draw main particle
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(129, 230, 217, ${this.opacity})`; // teal pixel
        ctx.fill();

        // Add glow effect
        ctx.shadowBlur = 12;
        ctx.shadowColor = 'rgba(129, 230, 217, 0.9)';
        ctx.fill();
        ctx.shadowBlur = 0;
      }
    }

    // Fast Star class (shooting stars with long tails)
    class FastStar {
      constructor() {
        this.reset();
        this.active = false; // Start inactive
      }

      reset() {
        this.active = false;
        this.x = Math.random() * canvas.width;
        this.y = -20;
        this.size = Math.random() * 1.5 + 1;
        // Diagonal movement - can go in any direction
        const angle = Math.random() * Math.PI * 0.5 + Math.PI * 0.25; // 45-135 degrees (diagonal down)
        const speed = Math.random() * 3 + 4; // Much faster
        this.speedX = Math.cos(angle) * speed;
        this.speedY = Math.sin(angle) * speed;
        this.opacity = 0.9;
        this.trail = [];
        this.maxTrailLength = 30; // Long tail
        this.lifetime = 0;
        this.maxLifetime = 300; // How long it stays visible
      }

      activate() {
        this.active = true;
        // Random starting position from top or left edge
        if (Math.random() > 0.5) {
          this.x = Math.random() * canvas.width;
          this.y = -20;
        } else {
          this.x = -20;
          this.y = Math.random() * canvas.height;
        }
        // Diagonal movement
        const angle = Math.random() * Math.PI * 0.5 + Math.PI * 0.25; // 45-135 degrees
        const speed = Math.random() * 3 + 4;
        this.speedX = Math.cos(angle) * speed;
        this.speedY = Math.sin(angle) * speed;
        this.lifetime = 0;
        this.trail = [];
      }

      update() {
        if (!this.active) return;
        
        this.lifetime++;
        
        // Add current position to trail
        this.trail.push({ x: this.x, y: this.y, opacity: this.opacity });
        if (this.trail.length > this.maxTrailLength) {
          this.trail.shift();
        }

        this.x += this.speedX;
        this.y += this.speedY;

        // Reset if off screen or lifetime expired
        if ((this.y > canvas.height + 50 || this.x > canvas.width + 50 || this.lifetime > this.maxLifetime)) {
          this.reset();
        }
      }

      draw() {
        if (!this.active) return;
        
        // Draw long tail streak
        if (this.trail.length > 1) {
          for (let i = 0; i < this.trail.length - 1; i++) {
            const point = this.trail[i];
            const nextPoint = this.trail[i + 1];
            const progress = i / this.trail.length;
            const trailOpacity = this.opacity * (1 - progress) * 0.9;
            const trailWidth = this.size * (1 - progress * 0.8);
            
            ctx.beginPath();
            ctx.moveTo(point.x, point.y);
            ctx.lineTo(nextPoint.x, nextPoint.y);
            ctx.strokeStyle = `rgba(255, 255, 255, ${trailOpacity})`;
            ctx.lineWidth = trailWidth;
            ctx.lineCap = 'round';
            ctx.shadowBlur = 12;
            ctx.shadowColor = 'rgba(99, 102, 241, 0.8)';
            ctx.stroke();
            ctx.shadowBlur = 0;
          }
        }

        // Draw bright star head
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(255, 255, 255, ${this.opacity})`;
        ctx.fill();
        
        // Add glow effect
        ctx.shadowBlur = 20;
        ctx.shadowColor = 'rgba(99, 102, 241, 1)';
        ctx.fill();
        ctx.shadowBlur = 0;
      }
    }

    // Create regular particles (enough to be clearly visible on all pages)
    const area = canvas.width * canvas.height;
    const baseDensity = area / 60000;
    const particleCount = Math.min(Math.max(Math.floor(baseDensity), 24), 48);
    for (let i = 0; i < particleCount; i++) {
      particles.push(new Particle());
    }

    // Create a few fast stars (shooting stars)
    const fastStars = [];
    const fastStarCount = 1; // Only one at a time
    for (let i = 0; i < fastStarCount; i++) {
      fastStars.push(new FastStar());
    }

    // Timer for spawning fast stars (roughly every 18-23 seconds)
    let lastFastStarTime = Date.now();
    const minFastStarInterval = 18000; // 18 seconds minimum
    const maxFastStarInterval = 23000; // 23 seconds maximum

    // Draw connections between nearby particles
    const drawConnections = () => {
      for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
          const dx = particles[i].x - particles[j].x;
          const dy = particles[i].y - particles[j].y;
          const distance = Math.sqrt(dx * dx + dy * dy);

          if (distance < 120) {
            const opacity = (1 - distance / 120) * 0.1;
            ctx.beginPath();
            ctx.moveTo(particles[i].x, particles[i].y);
            ctx.lineTo(particles[j].x, particles[j].y);
            ctx.strokeStyle = `rgba(99, 102, 241, ${opacity})`;
            ctx.lineWidth = 0.5;
            ctx.stroke();
          }
        }
      }
    };

    // Animation loop
    const animate = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      // Check if we should spawn a new fast star
      const now = Date.now();
      const timeSinceLastStar = now - lastFastStarTime;
      const nextStarInterval = minFastStarInterval + Math.random() * (maxFastStarInterval - minFastStarInterval);
      
      if (timeSinceLastStar >= nextStarInterval) {
        // Find an inactive fast star and activate it
        const inactiveStar = fastStars.find(star => !star.active);
        if (inactiveStar) {
          inactiveStar.activate();
          lastFastStarTime = now;
        }
      }

      // Draw connections first (behind particles)
      drawConnections();

      // Draw regular particles
      particles.forEach(particle => {
        particle.update();
        particle.draw();
      });

      // Draw fast stars (shooting stars)
      fastStars.forEach(star => {
        star.update();
        star.draw();
      });

      animationFrameId = requestAnimationFrame(animate);
    };

    animate();

    // Cleanup
    return () => {
      window.removeEventListener('resize', resizeCanvas);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      className="animated-background"
      aria-hidden="true"
    />
  );
};

export default AnimatedBackground;

