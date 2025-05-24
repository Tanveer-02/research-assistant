document.getElementById('generateButton').addEventListener('click', function () {
    const selectedValue = document.getElementById('actionSelect').value;

    if (!selectedValue) {
        alert("Please select an action from the dropdown menu.");
    } else {
        handleActionSelectChange(selectedValue);
        console.log("selectedValue: ", selectedValue);
    }
});


const handleActionSelectChange = async (action) => {
  const options = ["summarize", "suggest", "analyze", "explain", "extract"];

  if (options.includes(action)) {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });

    const [{ result: selectedText }] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: () => window.getSelection().toString()
    });

    console.log(selectedText)

    showLoading();

    // Send to backend
    fetch('http://localhost:8080/api/research/process', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ content: selectedText, operation: action })
    })
      .then(response => response.text())
      .then(data => {
        console.log('Response from backend:', data);
        const html = marked.parse(data);
        document.getElementById('resultBox').innerHTML = html;
      })
      .catch(error => {
        document.getElementById('resultBox').innerHTML = 'Error occurred while processing the request.';
        console.error('Error:', error);
      });
  }
}

document.getElementById('saveNoteButton').addEventListener('click', () => {

  const notes = document.getElementById('noteBox').value; 
  chrome.storage.local.set({ researchNotes: notes }, () => {
    alert('Notes saved successfully!');
  })
});

document.getElementById('deleteNoteButton').addEventListener('click', () => {

  chrome.storage.local.remove('researchNotes', () => {
    document.getElementById('noteBox').value = ''; 
  });
});

window.addEventListener('DOMContentLoaded', () => {

  chrome.storage.local.get('researchNotes', (data) => {
    if (data.researchNotes) {
      document.getElementById('noteBox').value = data.researchNotes;
    }
  });
});

const showLoading = () => {
    document.getElementById('resultBox').innerHTML = `
        <div class="flex justify-center items-center">
            <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
            <span class="ml-2">Loading...</span>
        </div>`;
};