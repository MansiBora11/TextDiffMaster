import React, { useState } from 'react';
import './App.css';

function App() {
  const [uploadType, setUploadType] = useState('');
  const [files, setFiles] = useState({
    file1: null,
    file2: null,
    folder1: [],
    folder2: [],
  });

  const handleOptionChange = (e) => {
    setUploadType(e.target.value);
  };

  const handleFileChange = (e) => {
    const { id, files } = e.target;
    setFiles((prev) => ({
      ...prev,
      [id]: id.includes('folder') ? Array.from(files) : files[0],
    }));
  };

  const handleSubmit = () => {
    if (uploadType === 'file') {
      if (!files.file1 || !files.file2) {
        alert('Please upload both files.');
        return;
      }
      alert('Files submitted successfully!');
    } else if (uploadType === 'folder') {
      if (!files.folder1.length || !files.folder2.length) {
        alert('Please upload both folders.');
        return;
      }
      alert('Folders submitted successfully!');
    } else {
      alert('Please select an option first.');
    }
  };

  return (
    <div className="container">
      <h1>File/Folder Comparison Tool</h1>
      <label htmlFor="uploadType">Choose an option:</label>
      <select id="uploadType" value={uploadType} onChange={handleOptionChange}>
        <option value="">--Select an option--</option>
        <option value="file">File</option>
        <option value="folder">Folder</option>
      </select>

      <div id="uploadSection" className="upload-section">
        {uploadType === 'file' && (
          <>
            <div className="upload-buttons">
              <label htmlFor="file1">File 1</label>
              <input type="file" id="file1" onChange={handleFileChange} />
            </div>
            <div className="upload-buttons">
              <label htmlFor="file2">File 2</label>
              <input type="file" id="file2" onChange={handleFileChange} />
            </div>
          </>
        )}
        {uploadType === 'folder' && (
          <>
            <div className="upload-buttons">
              <label htmlFor="folder1">Folder 1</label>
              <input
                type="file"
                id="folder1"
                webkitdirectory="true"
                directory="true"
                multiple
                onChange={handleFileChange}
              />
            </div>
            <div className="upload-buttons">
              <label htmlFor="folder2">Folder 2</label>
              <input
                type="file"
                id="folder2"
                webkitdirectory="true"
                directory="true"
                multiple
                onChange={handleFileChange}
              />
            </div>
          </>
        )}
      </div>
      <button className="submit-btn" onClick={handleSubmit}>
        Submit
      </button>
    </div>
  );
}

export default App;



