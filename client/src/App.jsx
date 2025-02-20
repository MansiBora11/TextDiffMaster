import React, { useState } from 'react';
import './App.css';
import fileService from './services/fileService';
import folderService from './services/folderService';

function App() {
  const [uploadType, setUploadType] = useState('file');
  const [files, setFiles] = useState({
    file1: null,
    file2: null,
    folder1: [],
    folder2: [],
  });
  const [loading, setLoading] = useState(false);
  const [downloadLinks, setDownloadLinks] = useState([]); // Stores download links for files with differences
  const [noDiffFiles, setNoDiffFiles] = useState([]); // Stores filenames of files with no differences

  const handleOptionChange = (e) => {
    setUploadType(e.target.value);
    setFiles({
      file1: null,
      file2: null,
      folder1: [],
      folder2: [],
    });
    setDownloadLinks([]);
    setNoDiffFiles([]); // Reset files with no differences
  };

  const handleFileChange = (e) => {
    const { id, files: uploadedFiles } = e.target;
    const fileArray = Array.from(uploadedFiles);
  
    // Limit folder uploads to 10 files
    if (id.includes('folder') && fileArray.length > 10) {
      alert(`You can upload a maximum of 10 files for ${id}.`);
      return;
    }

    setFiles((prev) => ({
      ...prev,
      [id]: id.includes('folder') ? fileArray : uploadedFiles[0], // Fix for single file inputs
    }));
};


const handleSubmit = async () => {
  try {
      setLoading(true);
      setDownloadLinks([]);
      setNoDiffFiles([]);

      let response;

      if (uploadType === "file") {
          const formData = new FormData();
          formData.append("file1", files.file1);
          formData.append("file2", files.file2);

          console.log("Submitting file comparison:", formData);
          response = await fileService.compareFiles(formData);
      } else if (uploadType === "folder") {
          files.folder1.forEach((file) => folderService.addFile("folder1", file));
          files.folder2.forEach((file) => folderService.addFile("folder2", file));

          console.log("Submitting folder comparison...");
          response = await folderService.submitFiles();
      }

      console.log("Raw Response from Backend:", response);
      console.log("Response Type:", typeof response);

      if (!response) {
          console.error("Received null or undefined response.");
          return;
      }

      if (typeof response === "string" && response.includes("Error")) {
          console.error("Backend returned an error message:", response);
          return;
      }

      if (uploadType === "file") {
          if (typeof response !== "string") {
              console.error("Unexpected response format for file:", response);
              return;
          }

          if (response.startsWith("No differences found in")) {
              setNoDiffFiles((prev) => [...prev, response.replace("No differences found in ", "")]);
          } else {
              setDownloadLinks((prev) => [...prev, response]);
          }
      } else if (uploadType === "folder") {
          if (!Array.isArray(response)) {
              console.error("Unexpected response format for folder:", response);
              return;
          }

          const noDiffFiles = response.filter((link) => link.startsWith("No differences found in"));
          const validLinks = response.filter((link) => !link.startsWith("No differences found in"));

          if (noDiffFiles.length > 0) {
              setNoDiffFiles(noDiffFiles.map((msg) => msg.replace("No differences found in ", "")));
          }

          setDownloadLinks(validLinks);
      }

      if (downloadLinks.length > 0) {
          console.log("Files with differences are available for download.");
      } else if (noDiffFiles.length > 0) {
          console.log("Some files had no differences.");
      } else {
          console.log("No differences found in any file.");
      }

  } catch (error) {
      console.error("Error in submit:", error);
  } finally {
      setLoading(false);
  }
};

return (
    <div className="container">
      <h1>File/Folder Comparison Tool</h1>
      
      <div className="form-group">
        <label htmlFor="uploadType">Choose an option:</label>
        <select 
          id="uploadType" 
          value={uploadType} 
          onChange={handleOptionChange}
          disabled={loading}
        >
          <option value="">--Select an option--</option>
          <option value="file">File</option>
          <option value="folder">Folder</option>
        </select>
      </div>

      <div id="uploadSection" className="upload-section">
        {uploadType === 'file' ? (
          <>
            <div className="form-group">
              <label>File 1</label>
              <input 
                type="file" 
                id="file1" 
                onChange={handleFileChange}
                disabled={loading} 
                style={{ display: 'none' }}
              />
              <button 
                onClick={() => document.getElementById('file1').click()}
                className="file-button"
              >
                Choose File
              </button>
              <span className="file-name">
                {files.file1 ? files.file1.name : ''}
              </span>
            </div>
            <div className="form-group">
              <label>File 2</label>
              <input 
                type="file" 
                id="file2" 
                onChange={handleFileChange}
                disabled={loading}
                style={{ display: 'none' }}
              />
              <button 
                onClick={() => document.getElementById('file2').click()}
                className="file-button"
              >
                Choose File
              </button>
              <span className="file-name">
                {files.file2 ? files.file2.name : ''}
              </span>
            </div>
          </>
        ) : (
          <>
            <div className="form-group">
              <label>Folder 1</label>
              <input 
                type="file" 
                id="folder1" 
                onChange={handleFileChange}
                webkitdirectory="true"
                directory="true"
                disabled={loading} 
                style={{ display: 'none' }}
              />
              <button 
                onClick={() => document.getElementById('folder1').click()}
                className="file-button"
              >
                Choose Folder
              </button>
              <span className="file-name">
                {files.folder1.length > 0 
                  ? `Selected ${files.folder1.length} files` 
                  : ''}
              </span>
            </div>
            <div className="form-group">
              <label>Folder 2</label>
              <input 
                type="file" 
                id="folder2" 
                onChange={handleFileChange}
                webkitdirectory="true"
                directory="true"
                disabled={loading}
                style={{ display: 'none' }}
              />
              <button 
                onClick={() => document.getElementById('folder2').click()}
                className="file-button"
              >
                Choose Folder
              </button>
              <span className="file-name">
                {files.folder2.length > 0 
                  ? `Selected ${files.folder2.length} files` 
                  : ''}
              </span>
            </div>
          </>
        )}
      </div>

      <button 
        className="submit-btn" 
        onClick={handleSubmit}
        disabled={loading || 
          (uploadType === 'file' && (!files.file1 || !files.file2)) ||
          (uploadType === 'folder' && (files.folder1.length === 0 || files.folder2.length === 0))}
      >
        {loading ? 'Processing...' : 'Submit'}
      </button>

      {/* Display Download Links for Folder Comparison */}
      {downloadLinks.length > 0 && (
        <div className="download-section">
          <h2>Download Diff Files:</h2>
          <ul>
            {downloadLinks.map((link, index) => (
              <li key={index}>
                <a href={link} target="_blank" rel="noopener noreferrer">
                  {link.split('/').pop()}
                </a>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Display Files with No Differences */}
      {noDiffFiles.length > 0 && (
        <div className="no-diff-section">
          <h2>Files with No Differences:</h2>
          <ul>
            {noDiffFiles.map((file, index) => (
              <li key={index}>{file}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

export default App;
