import { useRef, useMemo, useState } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls, PerspectiveCamera, Text, Html } from '@react-three/drei';
import * as THREE from 'three';

// Disaster marker component
const DisasterMarker = ({ disaster, onClick, isSelected }) => {
  const meshRef = useRef();
  const [hovered, setHovered] = useState(false);

  useFrame((state) => {
    if (meshRef.current) {
      meshRef.current.rotation.y += 0.01;
      if (hovered || isSelected) {
        meshRef.current.scale.lerp(new THREE.Vector3(1.5, 1.5, 1.5), 0.1);
      } else {
        meshRef.current.scale.lerp(new THREE.Vector3(1, 1, 1), 0.1);
      }
    }
  });

  const color = useMemo(() => {
    switch (disaster.severity) {
      case 'critical':
        return '#ef4444'; // red
      case 'high':
        return '#f59e0b'; // orange
      case 'medium':
        return '#eab308'; // yellow
      case 'low':
        return '#22c55e'; // green
      default:
        return '#3b82f6'; // blue
    }
  }, [disaster.severity]);

  // Convert lat/lng to 3D coordinates (simplified projection)
  const position = useMemo(() => {
    const x = (disaster.longitude || 0) * 0.1;
    const z = (disaster.latitude || 0) * -0.1;
    const y = 0.5;
    return [x, y, z];
  }, [disaster.latitude, disaster.longitude]);

  return (
    <group position={position}>
      <mesh
        ref={meshRef}
        onClick={() => onClick(disaster)}
        onPointerOver={() => setHovered(true)}
        onPointerOut={() => setHovered(false)}
      >
        <coneGeometry args={[0.3, 1, 8]} />
        <meshStandardMaterial
          color={color}
          emissive={color}
          emissiveIntensity={isSelected ? 0.5 : 0.2}
        />
      </mesh>

      {/* Pulsing ring effect */}
      <mesh position={[0, 0, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.4, 0.5, 32]} />
        <meshBasicMaterial color={color} opacity={0.3} transparent />
      </mesh>

      {/* Hover label */}
      {hovered && (
        <Html distanceFactor={10}>
          <div className="bg-white dark:bg-gray-800 px-3 py-2 rounded shadow-lg border border-gray-200 dark:border-gray-700 text-sm whitespace-nowrap pointer-events-none">
            <div className="font-bold">{disaster.name}</div>
            <div className="text-xs text-gray-600 dark:text-gray-400">
              {disaster.type}
            </div>
          </div>
        </Html>
      )}
    </group>
  );
};

// Ground plane
const Ground = () => {
  return (
    <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.5, 0]} receiveShadow>
      <planeGeometry args={[100, 100]} />
      <meshStandardMaterial
        color="#1a1a1a"
        metalness={0.1}
        roughness={0.8}
      />
    </mesh>
  );
};

// Grid helper
const GridPlane = () => {
  return (
    <gridHelper args={[100, 50, '#444444', '#222222']} position={[0, -0.49, 0]} />
  );
};

// Scene component
const Scene = ({ disasters, selectedDisaster, onDisasterClick }) => {
  return (
    <>
      {/* Lighting */}
      <ambientLight intensity={0.5} />
      <directionalLight
        position={[10, 10, 5]}
        intensity={1}
        castShadow
        shadow-mapSize-width={2048}
        shadow-mapSize-height={2048}
      />
      <pointLight position={[-10, 10, -10]} intensity={0.5} />

      {/* Ground and Grid */}
      <Ground />
      <GridPlane />

      {/* Disaster Markers */}
      {disasters.map((disaster) => (
        <DisasterMarker
          key={disaster.id}
          disaster={disaster}
          onClick={onDisasterClick}
          isSelected={selectedDisaster?.id === disaster.id}
        />
      ))}

      {/* Camera Controls */}
      <OrbitControls
        enableDamping
        dampingFactor={0.05}
        minDistance={5}
        maxDistance={50}
        maxPolarAngle={Math.PI / 2}
      />

      {/* Camera */}
      <PerspectiveCamera makeDefault position={[0, 10, 20]} fov={60} />
    </>
  );
};

const DisasterViewer3D = ({ disasters = [], selectedDisaster, onDisasterClick }) => {
  return (
    <div className="w-full h-full bg-gray-900">
      <Canvas
        shadows
        gl={{ antialias: true }}
        onCreated={({ gl }) => {
          gl.shadowMap.enabled = true;
          gl.shadowMap.type = THREE.PCFSoftShadowMap;
        }}
      >
        <Scene
          disasters={disasters}
          selectedDisaster={selectedDisaster}
          onDisasterClick={onDisasterClick}
        />
      </Canvas>

      {/* Legend */}
      <div className="absolute bottom-4 left-4 bg-white dark:bg-gray-800 rounded-lg shadow-lg p-4">
        <h3 className="font-bold mb-3">Severity Levels</h3>
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 rounded-full bg-danger-500"></div>
            <span className="text-sm">Critical</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 rounded-full bg-orange-500"></div>
            <span className="text-sm">High</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 rounded-full bg-yellow-500"></div>
            <span className="text-sm">Medium</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-4 h-4 rounded-full bg-success-500"></div>
            <span className="text-sm">Low</span>
          </div>
        </div>
        <div className="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700 text-xs text-gray-600 dark:text-gray-400">
          Click markers for details • Drag to rotate • Scroll to zoom
        </div>
      </div>
    </div>
  );
};

export default DisasterViewer3D;
