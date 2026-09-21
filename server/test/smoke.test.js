const assert = require('assert');
const express = require('express');
const http = require('http');
const fs = require('fs');
const path = require('path');
const jwt = require('jsonwebtoken');
const dotenv = require('dotenv');

dotenv.config({ path: path.resolve(__dirname, '../.env') });

const { initDb, get, all } = require('../src/database/db');
const tablesRouter = require('../src/routes/tables');
const groupsRouter = require('../src/routes/groups');
const requestsRouter = require('../src/routes/requests');

const JWT_SECRET = process.env.JWT_SECRET || 'super_secret_bankpoker_key_change_in_production';

// Web parser replica matching TableDetail.jsx logic
function parseTableDetailResponse(rawRes) {
    const resData = rawRes?.data && typeof rawRes.data === 'object' && !Array.isArray(rawRes.data)
        ? rawRes.data
        : rawRes;
    const t = resData?.table || resData?.data || (resData?.id ? resData : null);

    if (!t || !t.id) {
        return { notFound: true, table: null };
    }
    return { notFound: false, table: t };
}

async function runSmokeTests() {
    console.log('=== BANKPOKER SCHEMA & TABLE DETAIL SMOKE SUITE ===\n');

    await initDb();

    const app = express();
    app.use(express.json());
    app.use('/api/tables', tablesRouter);
    app.use('/api/groups', groupsRouter);
    app.use('/api/requests', requestsRouter);

    const server = http.createServer(app);
    await new Promise(resolve => server.listen(0, resolve));
    const port = server.address().port;
    const baseUrl = `http://127.0.0.1:${port}`;

    try {
        // Query an existing user and table from database for live integration tests
        const userRow = await get("SELECT id, username FROM users LIMIT 1");
        const testUser = userRow || { id: 'test-user-id', username: 'testadmin' };
        const validToken = jwt.sign({ id: testUser.id, username: testUser.username, role: 'ADMIN' }, JWT_SECRET);

        const tableRow = await get("SELECT id, name FROM tables WHERE is_deleted = 0 LIMIT 1");
        const existingTableId = tableRow ? tableRow.id : null;
        assert(existingTableId, 'Database must contain at least one non-deleted table for testing');

        // -------------------------------------------------------------
        // Assertion 1: GET /api/tables/:id with valid token returns 200 and top-level table object
        // -------------------------------------------------------------
        console.log('[ASSERTION 1] GET /api/tables/:id returns 200 with top-level table object');
        const res1 = await fetch(`${baseUrl}/api/tables/${existingTableId}`, {
            headers: { 'Authorization': `Bearer ${validToken}` }
        });
        assert.strictEqual(res1.status, 200, `Expected status 200, received ${res1.status}`);
        const body1 = await res1.json();
        assert(body1.table, 'Expected body to have top-level "table" property');
        assert.strictEqual(body1.table.id, existingTableId, `Expected table.id to match ${existingTableId}`);
        assert(typeof body1.table.name === 'string', 'Expected table.name to be a string');
        console.log(`  ✓ 200 OK with table ID: ${body1.table.id}, name: "${body1.table.name}"`);

        // -------------------------------------------------------------
        // Assertion 2: Web parser accepts { table: {...} } without players[] and does NOT produce notFound
        // -------------------------------------------------------------
        console.log('[ASSERTION 2] Web parser mock accepts { table: {...} } without players[] and does not produce notFound');
        const prodSamplePayload = {
            table: {
                id: "e06487f0-7923-4705-a249-bdad55a414d7",
                groupId: "a5d501fd-d026-4559-bc9a-95d804d038a2",
                name: "test5",
                code: "UGST98",
                chipValue: null,
                status: "ACTIVE",
                isActive: true,
                playerCount: 3
            },
            myStats: null
        };
        const parsedSample = parseTableDetailResponse(prodSamplePayload);
        assert.strictEqual(parsedSample.notFound, false, 'Parser produced notFound: true on valid table object');
        assert.strictEqual(parsedSample.table.id, "e06487f0-7923-4705-a249-bdad55a414d7", 'Parser extracted incorrect table.id');
        assert.strictEqual(parsedSample.table.name, "test5", 'Parser extracted incorrect table.name');

        // Also verify nested axios-style { data: { table: {...} } }
        const parsedAxios = parseTableDetailResponse({ data: prodSamplePayload });
        assert.strictEqual(parsedAxios.notFound, false, 'Parser failed on axios wrapped response');
        assert.strictEqual(parsedAxios.table.id, "e06487f0-7923-4705-a249-bdad55a414d7");

        // Also verify direct table { id: "direct-id" }
        const parsedDirect = parseTableDetailResponse({ id: "direct-123", name: "Direct" });
        assert.strictEqual(parsedDirect.notFound, false, 'Parser failed on direct table response');
        assert.strictEqual(parsedDirect.table.id, "direct-123");

        // Verify invalid / empty payload produces notFound: true
        const parsedEmpty = parseTableDetailResponse({});
        assert.strictEqual(parsedEmpty.notFound, true, 'Parser should produce notFound: true on empty object');
        console.log('  ✓ Web parser correctly unwraps { table: {...} } without requiring players[]');

        // -------------------------------------------------------------
        // Assertion 3: 404 only for truly missing/soft-deleted table
        // -------------------------------------------------------------
        console.log('[ASSERTION 3] 404 only for truly missing/soft-deleted table');
        const nonExistentId = 'non-existent-table-0000-000000000000';
        const res3 = await fetch(`${baseUrl}/api/tables/${nonExistentId}`, {
            headers: { 'Authorization': `Bearer ${validToken}` }
        });
        assert.strictEqual(res3.status, 404, `Expected status 404 for missing table, received ${res3.status}`);
        const body3 = await res3.json();
        assert.strictEqual(body3.error, 'Table not found');
        console.log('  ✓ Server returns 404 for truly missing table');

        // -------------------------------------------------------------
        // Assertion 4: 401 for missing/invalid token, not Table Not Found
        // -------------------------------------------------------------
        console.log('[ASSERTION 4] 401 for missing/invalid token, not Table Not Found');
        // Missing token
        const res4NoAuth = await fetch(`${baseUrl}/api/tables/${existingTableId}`);
        assert.strictEqual(res4NoAuth.status, 401, `Expected status 401 for missing auth, received ${res4NoAuth.status}`);
        const body4NoAuth = await res4NoAuth.json();
        assert(body4NoAuth.error.includes('Unauthorized'), `Expected unauthorized error message, got: ${body4NoAuth.error}`);

        // Invalid token
        const res4Invalid = await fetch(`${baseUrl}/api/tables/${existingTableId}`, {
            headers: { 'Authorization': 'Bearer bad_malformed_token' }
        });
        assert.strictEqual(res4Invalid.status, 401, `Expected status 401 for invalid auth, received ${res4Invalid.status}`);
        console.log('  ✓ Server returns 401 Unauthorized when unauthenticated');

        // -------------------------------------------------------------
        // Assertion 5: Primary 200 with { table:{...}, myStats:null } resolves loading to false and shell renders
        // -------------------------------------------------------------
        console.log('[ASSERTION 5] Primary 200 with { table:{...}, myStats:null } resolves loading to false');
        function simulateTableDataLoader(primaryPayload, auxPayloads = {}) {
            let state = {
                table: null,
                loading: true,
                errorTitle: '',
                error: '',
                players: [],
                activity: { buyIns: [], exits: [] },
            };

            const t = primaryPayload?.table || primaryPayload?.data || (primaryPayload?.id ? primaryPayload : null);
            if (!t || !t.id) {
                state.errorTitle = 'Table Not Found';
                state.loading = false;
                return state;
            }

            // Immediately set table and clear loading
            state.table = { ...t };
            state.loading = false;

            // Aux: players
            try {
                const pData = auxPayloads.players;
                const pList = Array.isArray(pData) ? pData : (Array.isArray(pData?.players) ? pData.players : []);
                state.players = pList;
            } catch (e) {
                state.players = [];
            }

            // Aux: activity
            try {
                const bData = auxPayloads.buyIns;
                const eData = auxPayloads.exits;
                state.activity.buyIns = Array.isArray(bData) ? bData : (Array.isArray(bData?.buyIns) ? bData.buyIns : []);
                state.activity.exits = Array.isArray(eData) ? eData : (Array.isArray(eData?.exits) ? eData.exits : []);
            } catch (e) {
                state.activity = { buyIns: [], exits: [] };
            }

            return state;
        }

        const simResult = simulateTableDataLoader(prodSamplePayload);
        assert.strictEqual(simResult.loading, false, 'Loading must be false after table resolves');
        assert(simResult.table && simResult.table.id, 'Table must be set');
        assert.strictEqual(simResult.players.length, 0, 'Players slice correctly defaults to empty array');
        console.log('  ✓ Primary payload unwraps table and immediately resolves loading to false');

        // -------------------------------------------------------------
        // Assertion 6: Aux endpoint returning 404/500/non-array does NOT keep loading stuck and does NOT crash
        // -------------------------------------------------------------
        console.log('[ASSERTION 6] Aux endpoints returning 404/500/non-array do NOT hang loading or crash');
        const brokenAuxLoads = [
            { players: null, buyIns: undefined, exits: 'error 500' },
            { players: { error: 'Internal server error' }, buyIns: 500, exits: {} },
            { players: '<html>404 Not Found</html>', buyIns: [], exits: [] }
        ];

        for (const badAux of brokenAuxLoads) {
            const badLoadResult = simulateTableDataLoader(prodSamplePayload, badAux);
            assert.strictEqual(badLoadResult.loading, false, 'Loading must resolve to false even if aux fails');
            assert(badLoadResult.table, 'Table object must remain intact despite aux failures');
            assert(Array.isArray(badLoadResult.players), 'Players must safely fallback to array');
            assert(Array.isArray(badLoadResult.activity.buyIns), 'BuyIns must safely fallback to array');
            assert(Array.isArray(badLoadResult.activity.exits), 'Exits must safely fallback to array');
        }
        console.log('  ✓ Auxiliary failures gracefully fallback to [] without hanging or crashing');

        // -------------------------------------------------------------
        // Assertion 7: myStats null does not throw in balance calculations
        // -------------------------------------------------------------
        console.log('[ASSERTION 7] myStats null does not throw in balance/stat calculations');
        function calculateUserStats(tableObj, myPlayer = null) {
            const myTotalBuyIns = tableObj?.myStats && tableObj.myStats.totalBuyIns != null
                ? Number(tableObj.myStats.totalBuyIns)
                : 0;
            const myTotalExits = tableObj?.myStats && tableObj.myStats.totalExits != null
                ? Number(tableObj.myStats.totalExits)
                : 0;
            const myNetBalance = tableObj?.myStats && tableObj.myStats.netBalance != null
                ? Number(tableObj.myStats.netBalance)
                : (myTotalExits - myTotalBuyIns);
            return { myTotalBuyIns, myTotalExits, myNetBalance };
        }

        const statsWithNull = calculateUserStats(prodSamplePayload.table);
        assert.strictEqual(statsWithNull.myTotalBuyIns, 0);
        assert.strictEqual(statsWithNull.myTotalExits, 0);
        assert.strictEqual(statsWithNull.myNetBalance, 0);
        console.log('  ✓ table.myStats: null is handled safely without throwing');

        // -------------------------------------------------------------
        // Assertion 8: /api/* is bypassed and Service Worker reload loop is guarded
        // -------------------------------------------------------------
        console.log('[ASSERTION 8] Service worker v3 cache rules and once-only reload loop guard');
        const swPath = path.resolve(__dirname, '../../web/public/sw.js');
        const swContent = fs.readFileSync(swPath, 'utf8');
        const mainPath = path.resolve(__dirname, '../../web/src/main.jsx');
        const mainContent = fs.readFileSync(mainPath, 'utf8');

        assert(swContent.includes("const CACHE_VERSION = 'v3'"), 'Expected sw.js to use CACHE_VERSION v3');
        assert(swContent.includes("url.pathname.startsWith('/api')"), 'Expected sw.js to bypass /api routes from cache');
        assert(swContent.includes("cache: 'no-cache'"), 'Expected sw.js navigate requests to use cache: no-cache');
        assert(swContent.includes("caches.delete(key)"), 'Expected sw.js to purge obsolete caches on activate');

        assert(mainContent.includes('didReload'), 'Expected main.jsx to have didReload once-only guard');
        assert(mainContent.includes('bp_sw_reload_ts'), 'Expected main.jsx to debounce reloads with sessionStorage');
        console.log('  ✓ sw.js verifies CACHE_VERSION v3 and main.jsx verifies once-only reload guard');

        console.log('\n=== ALL SMOKE TEST ASSERTIONS PASSED SUCCESSFULLY ===');
    } finally {
        await new Promise(resolve => server.close(resolve));
    }
}

runSmokeTests()
    .then(() => {
        const { db } = require('../src/database/db');
        db.close((err) => {
            if (err) console.error('DB close error:', err);
            // Let the Node event loop exit cleanly
        });
    })
    .catch((err) => {
        console.error('\nSMOKE TEST FAILED:', err);
        process.exit(1);
    });
