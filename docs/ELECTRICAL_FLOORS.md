# Electrical Floors & Energy Network

FactoryCore uses a graph-based energy network called **Factory Network**.

## Core Concept
- **Energy Islands:** Power is not stored in individual blocks but in a virtual "ElectricalNetwork" object.
- **Floors as Nodes:** `ElectricalFloorBlock` acts as a node. Placing floors adjacent to each other merges them into the same network.
- **Shared Buffer:** All machines connected to the same floor network share a single energy buffer (1,000,000 FE default).

## How it works
1. **Placement:** When you place an Electrical Floor, it checks 6 neighbors.
   - If adjacent to an existing network, it joins it.
   - If adjacent to multiple different networks, it **merges** them into one super-network.
   - If isolated, it creates a new network ID.
2. **Persistence:** `FactoryNetworkManager` (SavedData) saves the state of all networks to `level.dat`.
3. **Usage:** Machines query the floor below them for `IEnergyStorage` capability. The Floor Block Entity delegates this query to the Network Manager.

## I/O Logic
- Machines (Multiblocks) do not need internal buffers if they are purely "consumers" on the grid. They can just pull from the floor.
- Generators push to the floor.
- **Limit:** 10k FE/t transfer rate per operation.

## Technical Details
- **Manager:** `FactoryNetworkManager`
- **Logic:** `ElectricalNetwork`
- **Block:** `ElectricalFloorBlock`
- **Capability:** `ElectricalFloorBlockEntity` exposes `IEnergyStorage`.
