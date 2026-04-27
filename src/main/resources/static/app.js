let selectedContactId = "";

document.addEventListener('DOMContentLoaded', () => {
    loadConversations();

    // Auto-refresh messages every 3 seconds if a chat is open
    setInterval(() => {
        if (selectedContactId) {
            refreshMessages(selectedContactId);
        }
    }, 3000);

    // Enter key support for chat
    document.getElementById('chat-input').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') sendMessage();
    });
});

async function loadConversations() {
    const listContainer = document.getElementById('conversation-list');
    try {
        const response = await fetch('/api/v1/admin/contacts');
        const contacts = await response.json();
        listContainer.innerHTML = '';
        contacts.forEach(contact => {
            const displayName = (contact.name && contact.name !== 'default') ? contact.name : `ID: ${contact.id.substring(0,8)}`;
            const item = document.createElement('div');
            item.className = `p-3 flex items-center gap-3 cursor-pointer hover:bg-gray-50 border-b border-gray-100 transition`;
            item.innerHTML = `
                <div class="w-10 h-10 bg-blue-100 text-[#0084FF] rounded-full flex items-center justify-center font-bold">${displayName.charAt(0)}</div>
                <div class="flex-1"><p class="font-bold text-sm truncate">${displayName}</p></div>
            `;
            item.onclick = (e) => selectContact(contact.id, displayName, e);
            listContainer.appendChild(item);
        });
    } catch (err) { console.error("Contacts fail:", err); }
}

async function selectContact(id, name, event) {
    selectedContactId = id;
    document.getElementById('active-user-name').innerText = name;
    document.getElementById('header-avatar').innerText = name.charAt(0);

    // Highlight selection
    document.querySelectorAll('#conversation-list > div').forEach(el => el.classList.remove('bg-blue-50'));
    event.currentTarget.classList.add('bg-blue-50');

    refreshMessages(id);
}

async function refreshMessages(contactId) {
    const chatBubbles = document.getElementById('chat-bubbles');
    try {
        const response = await fetch(`/api/v1/admin/messages/${contactId}`);
        const messages = await response.json();

        // Check if we need to update the UI
        if (chatBubbles.childElementCount !== messages.length) {
            chatBubbles.innerHTML = '';

            messages.forEach(msg => {
                const bubble = document.createElement('div');
                const isFromCRM = msg.direction === 'OUTBOUND';

                if (isFromCRM) {
                    bubble.className = "p-3 rounded-2xl rounded-tr-none self-end max-w-[75%] text-sm shadow-sm mb-1 " +
                        (msg.content.includes("RENDER_ORDER_CARD") ? "bg-white border-2 border-blue-500 text-gray-900" : "bg-[#0084FF] text-white");
                } else {
                    bubble.className = "bg-[#E4E6EB] text-gray-900 p-3 rounded-2xl rounded-tl-none self-start max-w-[75%] text-sm shadow-sm mb-1";
                }

                // INTERCEPT THE SIGNAL
                if (msg.content.includes("RENDER_ORDER_CARD|")) {
                    try {
                        const jsonPart = msg.content.split("|")[1];
                        const orderData = JSON.parse(jsonPart);
                        bubble.innerHTML = renderOrderForm(orderData);
                    } catch (e) {
                        bubble.innerText = msg.content; // Fallback if JSON is messy
                    }
                } else {
                    bubble.innerText = msg.content;
                }

                chatBubbles.appendChild(bubble);
            });

            // Smoothly scroll to the latest message
            chatBubbles.scrollTo({ top: chatBubbles.scrollHeight, behavior: 'smooth' });
        }
    } catch (err) {
        console.error("Failed to load message history:", err);
    }
}

async function sendMessage() {
    const input = document.getElementById('chat-input');
    const text = input.value.trim();

    if (!text || !selectedContactId) return;

    // This perfectly matches your CRMResponse DTO
    const payload = {
        recipient: { id: selectedContactId },
        message: { text: text }
    };

    try {
        // Updated to the base path of your Outbound controller
        const response = await fetch('/api/outbound', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            input.value = ''; // Clear input field
            // Re-fetch the messages from H2 so the new blue bubble appears
            refreshMessages(selectedContactId);
        } else {
            console.error("Server responded with error:", response.status);
        }
    } catch (err) {
        console.error("Outbound send failed:", err);
    }
}

// AI Assist Function
async function sendAiCommand() {
    const input = document.getElementById('ai-input');
    const command = input.value.trim();
    if (!command || !selectedContactId) return;

    try {
        const response = await fetch('/api/v1/admin/ai-assistant', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: command, selectedContactId: selectedContactId })
        });
        const result = await response.text();
        const log = document.getElementById('ai-log');
        log.innerHTML += `<div class="bg-white p-2 rounded text-xs border border-blue-100 mb-2"><strong>AI:</strong> ${result}</div>`;
        input.value = '';
        loadConversations(); // Refresh sidebar in case name changed
    } catch (err) { console.error("AI fail:", err); }
}

function renderOrderForm(data) {
    return `
        <div class="flex flex-col gap-2 min-w-[200px]">
            <div class="flex items-center gap-2 border-b pb-1 mb-1">
                <span class="text-blue-600 font-bold">🛒 Draft Order</span>
            </div>
            <div class="space-y-1 text-xs">
                <p><strong>SKU:</strong> ${data.sku}</p>
                <p><strong>Size:</strong> ${data.size}</p>
                <p><strong>Qty:</strong> ${data.qty}</p>
                <p><strong>Address:</strong> ${data.address}</p>
            </div>
            <button onclick="confirmDraftOrder('${data.sku}')" 
                    class="mt-2 bg-green-500 hover:bg-green-600 text-white font-bold py-1 px-2 rounded transition text-xs">
                Confirm & Send to Customer
            </button>
        </div>
    `;
}

// Placeholder for when you eventually want to save
function confirmDraftOrder(sku) {
    alert("Order for " + sku + " confirmed! (Next step: implement save to DB)");
}