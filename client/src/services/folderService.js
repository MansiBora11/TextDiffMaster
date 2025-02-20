class FolderService {
    constructor() {
      this.folder1Files = [];
      this.folder2Files = [];
      this.maxFiles = 10;
    }
  
    addFile(folder, file) {
      if (folder === "folder1") {
        if (this.folder1Files.length >= this.maxFiles) {
          throw new Error("Folder 1 can only contain up to 10 files.");
        }
        this.folder1Files.push(file);
      } else if (folder === "folder2") {
        if (this.folder2Files.length >= this.maxFiles) {
          throw new Error("Folder 2 can only contain up to 10 files.");
        }
        this.folder2Files.push(file);
      } else {
        throw new Error("Invalid folder specified.");
      }
    }
  
    async submitFiles() {
        const formData = new FormData();
        this.folder1Files.forEach(file => formData.append("folder1Files", file));
        this.folder2Files.forEach(file => formData.append("folder2Files", file));
    
        try {
            const response = await fetch(`https://textdiffmaster.onrender.com/api/compareFolders`, {
                method: "POST",
                body: formData
            });
    
            if (!response.ok) {
                throw new Error(`Server error: ${response.status}`);
            }
    
            const data = await response.json();  // 🔹 Force JSON parsing
            console.log("API Response Data:", data);  // 🔹 Debugging
    
            return data;
        } catch (error) {
            console.error("Error submitting files:", error);
            return [];
        }
    }
    
}
  
  export default new FolderService();
  